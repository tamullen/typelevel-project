package com.tamullen.jobsboard.domain

import doobie.util._
import doobie.util.meta._

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

}