package com.tamullen.jobsboard.domain

import doobie.util.*
import doobie.util.meta.*
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

object user {
  final case class User(
                     email: String,
                     hashedPassword: String,
                     firstName: Option[String],
                     lastName: Option[String],
                     company: Option[String],
                     role: Role
                   )

  final case class NewUserInfo(email: String,
                               password: String,
                               firstName: Option[String],
                               lastName: Option[String],
                               company: Option[String]
                               )

  enum Role {
    case ADMIN, RECRUITER
  }

  object Role {
    given metaRole: Meta[Role] = Meta[String].timap[Role](Role.valueOf)(_.toString)
  }

  given roleAuthEnum: SimpleAuthEnum[Role, String] with {
    override val values: AuthGroup[Role] = AuthGroup(Role.ADMIN, Role.RECRUITER)
    override def getRepr(t: Role): String = t.toString
  }

}
