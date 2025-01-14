package io.jongyun.graphinstagram.service.post

import io.jongyun.graphinstagram.entity.hashtag.Hashtag
import io.jongyun.graphinstagram.entity.hashtag.HashtagRepository
import io.jongyun.graphinstagram.entity.member.Member
import io.jongyun.graphinstagram.entity.member.MemberRepository
import io.jongyun.graphinstagram.entity.post.Post
import io.jongyun.graphinstagram.entity.post.PostCustomRepository
import io.jongyun.graphinstagram.entity.post.PostRepository
import io.jongyun.graphinstagram.exception.BusinessException
import io.jongyun.graphinstagram.exception.ErrorCode
import io.jongyun.graphinstagram.types.CreatePostInput
import io.jongyun.graphinstagram.types.PostPageInput
import io.jongyun.graphinstagram.types.UpdatePostInput
import io.jongyun.graphinstagram.util.mapToGraphql
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import io.jongyun.graphinstagram.types.Post as TypesPost

@Transactional
@Service
class PostService(
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val postCustomRepository: PostCustomRepository,
    private val hashTagRepository: HashtagRepository
) {

    fun createPost(memberId: Long, createPostInput: CreatePostInput): Boolean {
        contentValidation(createPostInput.content)
        val member = findMemberById(memberId)
        val post = Post(
            createdBy = member,
            content = createPostInput.content
        )
        val hashtagList = getHashtagList(createPostInput.tags ?: emptyList())
        post.addAllHashTag(hashtagList)
        postRepository.save(post)
        return true
    }


    @Transactional(readOnly = true)
    fun getPost(postId: Long): TypesPost {
        val post = findPostById(postId)
        return mapToGraphql(post)
    }

    @Transactional(readOnly = true)
    fun getMyPost(memberId: Long): List<Post> {
        val member = findMemberById(memberId)
        return postRepository.findByCreatedBy(member)
    }

    @Transactional(readOnly = true)
    fun getAll(): List<TypesPost> {
        return postRepository.findAll().map { mapToGraphql(it) }
    }

    @Transactional(readOnly = true)
    fun getAllMyLikedPostByMemberId(memberId: Long): List<TypesPost> {
        return postCustomRepository.findAllMyLikedPostByMember(findMemberById(memberId))
            .map { mapToGraphql(it) }
    }

    fun updatePost(memberId: Long, updatePostInput: UpdatePostInput): Boolean {
        val member = findMemberById(memberId)
        val post = postRepository.findByCreatedByAndId(member, updatePostInput.postId.toLong())
            ?: throw BusinessException(
                ErrorCode.POST_DOES_NOT_EXISTS,
                "게시물을 찾을 수 없습니다. ID: ${updatePostInput.postId}"
            )
        contentValidation(updatePostInput.content)
        post.content = updatePostInput.content
        postRepository.save(post)
        return true
    }

    fun deletePost(memberId: Long, postId: Long): Boolean {
        val member = findMemberById(memberId)
        val post = postRepository.findByCreatedByAndId(member, postId) ?: throw BusinessException(
            ErrorCode.POST_DOES_NOT_EXISTS, "post 를 찾을 수없습니다. post id: $postId"
        )
        postRepository.delete(post)
        return true
    }

    @Transactional(readOnly = true)
    fun findAllByHashtag(hashtag: String, postPageInput: PostPageInput): List<Post> {
        val hashtag = hashTagRepository.findByTagName(hashtag) ?: throw BusinessException(
            ErrorCode.HASHTAG_DOES_NOT_EXISTS,
            "해시태그를 찾을 수 없습니다."
        )
        return postCustomRepository.findAllByHashtag(hashtag, postPageInput)
    }


    private fun contentValidation(content: String) {
        when {
            !StringUtils.hasText(content) ->
                throw BusinessException(ErrorCode.POST_CONTENT_IS_REQUIRED, "게시물의 컨텐츠 내용은 필수입니다.")
            content.length > 100 ->
                throw BusinessException(ErrorCode.CONTENT_MUST_BE_100_LENGTH_OR_LESS, "컨텐츠 내용은 100자 이하여야 합니다.")
        }
    }

    private fun findMemberById(memberId: Long): Member {
        val member = memberRepository.findById(memberId).orElseThrow {
            BusinessException(ErrorCode.MEMBER_DOES_NOT_EXISTS, "계정을 찾을 수 없습니다.")
        }
        return member
    }

    private fun findPostById(postId: Long) = postRepository.findById(postId).orElseThrow {
        BusinessException(
            ErrorCode.POST_DOES_NOT_EXISTS,
            "게시물을 찾을 수 없습니다. ID: $postId"
        )
    }

    private fun getHashtagList(tags: List<String>): List<Hashtag> {
        if (tags.isEmpty()) {
            return emptyList()
        }

        val hashtags = hashTagRepository.findAllByTagNameIn(tags)
        val tagNames = hashtags.map { it.tagName }
        return tags.map { tag ->
            if (tagNames.contains(tag)) {
                hashtags.first { it.tagName == tag }
            } else {
                Hashtag(tagName = tag)
            }
        }
    }

}
