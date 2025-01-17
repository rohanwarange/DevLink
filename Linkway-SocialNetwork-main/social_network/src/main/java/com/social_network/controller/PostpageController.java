package com.social_network.controller;

import com.social_network.dto.request.CommentDTO;
import com.social_network.entity.Comment;
import com.social_network.entity.Post;
import com.social_network.entity.Tag;
import com.social_network.entity.User;
import com.social_network.service.*;
import com.social_network.util.MarkdownRenderUtil;
import com.social_network.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Controller
@AllArgsConstructor
@CrossOrigin
public class PostpageController {

    private UserService userService;

    private SecurityUtil securityUtil;

    private PostService postService;

    private MarkdownRenderUtil markdownRenderUtil;

    private FollowService followService;

    private VoteService voteService;

    private CommentService commentService;

    @ModelAttribute("followingTags")
    public List<Tag> getFollowingTags() {
        List<Tag> followingTags = null;
        try {
            String username = Objects.requireNonNull(SecurityUtil.getCurrentUser()).getUsername();
            User user = userService.findByUsername(username).get();
            followingTags = user.getFollowingTags();
        } catch (NullPointerException ignored) {
        }
        return followingTags;
    }

    @GetMapping("/post/{postId}")
    public String showPostPage(@PathVariable("postId") int postId,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               Model model,
                               HttpServletRequest request){
        postService.increaseView(postId);
        Post post = postService.findById(postId);
        String htmlContent = markdownRenderUtil.convertToHtml(post.getContent());
        post.setHtmlContent(htmlContent);
        String currentUsername = securityUtil.getCurrentUser().getUsername();

        Page<Comment> rootComments = commentService.findRootCommentsByPost(post, page);

        for(Comment  comment : rootComments.stream().toList()){
            int voteType = voteService.getUserCommentVoteType(comment.getId(), (int)request.getSession().getAttribute("id"));
            comment.setUpvoted(voteType == 1);
            comment.setDownvoted(voteType == -1);

            String commentHtmlContent = markdownRenderUtil.convertToHtml(comment.getContent());
            comment.setHtmlContent(commentHtmlContent);
        }

        List<Post> coauthorPosts = postService.getRandowPostsByAuthor(post.getAuthor(), 3);

        int voteType = voteService.getUserPostVoteType(postId, (int)request.getSession().getAttribute("id"));
        long authorFollowers = followService.getFollowerCount(post.getAuthor());
        model.addAttribute("post", post);
        model.addAttribute("authorFollowers", authorFollowers);
        model.addAttribute("isCurrentUser",
                post.getAuthor().getUsername().equals(currentUsername));
        model.addAttribute("isFollowing",
                followService.isFollowing(currentUsername,
                        post.getAuthor().getUsername()));
        model.addAttribute("upvoted", voteType == 1);
        model.addAttribute("downvoted", voteType == -1);
        model.addAttribute("userComment", new CommentDTO());
        model.addAttribute("rootComments" ,rootComments);
        model.addAttribute("currentPage", page);
        model.addAttribute("coauthorPosts", coauthorPosts);
        return "home/postpage";
    }

    @PostMapping("/post/{postId}/{voteType}")
    public String votePost(@PathVariable("postId") int postId,
                           @PathVariable("voteType") String voteType,
                           HttpServletRequest request){
        String prevPath = request.getHeader("Referer");
        int voteChange = voteType.equals("upvote") ? 1 : -1;
        voteService.increasePostVote(postId, (int)request.getSession().getAttribute("id"), voteChange);
        return "redirect:" + prevPath;
    }

    @PostMapping("/post/{postId}/unvote")
    public String unvotePost(@PathVariable("postId") int postId,
                             HttpServletRequest request){
        String prevPath = request.getHeader("Referer");
        voteService.unvotePost(postId, (int)request.getSession().getAttribute("id"));
        return "redirect:" + prevPath;
    }

    @PostMapping("/post/{postId}/comment")
    public String comment(@PathVariable("postId") int postId,
                          @ModelAttribute("userComment") CommentDTO commentDTO,
                          HttpServletRequest request){
        String prevPath = request.getHeader("Referer");

        Comment newComment;

        if(commentDTO.getId() > 0){
            newComment = commentService.findById(commentDTO.getId());
            newComment.setContent(commentDTO.getContent());
            newComment.setWasUpdated(true);
        }
        else{
            newComment = new Comment();
            newComment.setCreatedAt(Date.from(Instant.now()));
            newComment.setContent(commentDTO.getContent());
            User author = userService.findById(commentDTO.getAuthorId());
            newComment.setAuthor(author);
            Post post = postService.findById(commentDTO.getPostId());
            newComment.setPost(post);
            if(commentDTO.getParentId() > 0){
                Comment parentComment = commentService.findById(commentDTO.getParentId());
                newComment.setParentComment(parentComment);
            }
        }
        commentService.saveComment(newComment);
        return "redirect:" + prevPath + "#post-comments";
    }

    @PostMapping("/comment/{commentId}/{voteType}")
    public String voteComment(@PathVariable("commentId") int commentId,
                              @PathVariable("voteType") String voteType,
                              HttpServletRequest request){
        String prevPath = request.getHeader("Referer");
        int voteChange = voteType.equals("upvote") ? 1 : -1;
        voteService.increaseCommentVote(commentId, (int)request.getSession().getAttribute("id"), voteChange);
        return "redirect:" + prevPath + "#post-comments";
    }

    @PostMapping("/comment/{commentId}/unvote")
    public String unvoteComment(@PathVariable("commentId") int commentId,
                                HttpServletRequest request){
        String prevPath = request.getHeader("Referer");
        voteService.unvoteComment(commentId, (int)request.getSession().getAttribute("id"));
        return "redirect:" + prevPath + "#post-comments";
    }

    @GetMapping("/api/comment/{parendId}/child")
    @ResponseBody
    public List<Comment> getChildComments(@PathVariable int parendId,
                                HttpServletRequest request){

        List<Comment> childComments = commentService.getChildComments(parendId);

        for(Comment comment : childComments){
            int voteType = voteService.getUserCommentVoteType(comment.getId(), (int)request.getSession().getAttribute("id"));
            comment.setUpvoted(voteType == 1);
            comment.setDownvoted(voteType == -1);

            String htmlContent = markdownRenderUtil.convertToHtml(comment.getContent());
            comment.setHtmlContent(htmlContent);
        }
        return childComments;
    }

    @PostMapping("/transfer")
    @ResponseBody
    public String tranferToHtmlContent(@RequestBody String content){
        String htmlContent = markdownRenderUtil.convertToHtml(content);
        System.out.println("ABCDEF: " + content);
        return htmlContent;
    }

    @GetMapping("/api/comment/{commentId}")
    @ResponseBody
    public Comment getCommentById(@PathVariable int commentId){
        Comment comment = commentService.findById(commentId);
        return comment;
    }

}
