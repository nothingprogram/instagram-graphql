package io.jongyun.graphinstagram.service.member

import io.jongyun.graphinstagram.entity.member.Member
import io.jongyun.graphinstagram.entity.member.MemberCustomRepository
import io.jongyun.graphinstagram.entity.member.MemberRepository
import io.jongyun.graphinstagram.entity.post.PostRepository
import io.jongyun.graphinstagram.exception.BusinessException
import io.jongyun.graphinstagram.exception.ErrorCode
import io.jongyun.graphinstagram.types.MemberRegisterInput
import io.jongyun.graphinstagram.util.mapToGraphql
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.jongyun.graphinstagram.types.Member as TypesMember

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val encoder: BCryptPasswordEncoder,
    private val memberCustomRepository: MemberCustomRepository,
    private val postRepository: PostRepository
) {

    @Transactional
    fun register(memberRegisterInput: MemberRegisterInput): Boolean {
        if (memberRepository.existsByName(memberRegisterInput.name)) {
            throw BusinessException(
                ErrorCode.ID_IS_DUPLICATE,
                "[register] member name is already exists: ${memberRegisterInput.name}"
            )
        }
        val registryMember = mapToEntity(memberRegisterInput)
        encoder.encode(registryMember.password).also { registryMember.password = it }
        memberRepository.save(registryMember)
        return true
    }

    @Transactional(readOnly = true)
    fun findMyInfo(memberId: Long): TypesMember {
        return mapToGraphql(findMemberById(memberId))
    }


    @Transactional(readOnly = true)
    fun findAllLikedMemberToPost(postId: Long): List<TypesMember> {
        val post = findPostById(postId)
        return memberCustomRepository.findAllLikedMemberToPost(post).map { mapToGraphql(it) }
    }

    private fun findPostById(postId: Long) = postRepository.findById(postId).orElseThrow {
        BusinessException(
            ErrorCode.POST_DOES_NOT_EXISTS,
            "게시물을 찾을 수 없습니다. ID: $postId"
        )
    }

    private fun findMemberById(memberId: Long): Member {
        val member = memberRepository.findById(memberId).orElseThrow {
            BusinessException(ErrorCode.MEMBER_DOES_NOT_EXISTS, "계정을 찾을 수 없습니다.")
        }
        return member
    }
}

private fun mapToEntity(memberRegisterInput: MemberRegisterInput): Member {
    return Member(name = memberRegisterInput.name, password = memberRegisterInput.password)
}

