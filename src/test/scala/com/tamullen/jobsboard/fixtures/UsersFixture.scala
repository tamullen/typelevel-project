package com.tamullen.jobsboard.fixtures

import com.tamullen.jobsboard.domain.user.*
import com.tamullen.jobsboard.domain.user.Role._

trait UsersFixture {
  val Travis = User(
      "travis@test.com",
      "tamullen",
      Some("Travis"),
      Some("Mullen"),
      Some("tamullen"),
      Role.ADMIN
    )
  
  val Amber = User(
    "amber@test.com",
    "amanley",
    Some("Amber"),
    Some("Manley"),
    Some("tamullen"),
    Role.RECRUITER
  )

  val NewUser = User(
    "newuser@gmail.com",
    "simplepassword",
    Some("New"),
    Some("User"),
    Some("test company"),
    RECRUITER
  )

  val UpdatedTravis = User(
    "travis@test.com",
    "tamullen",
    Some("Travis"),
    Some("Mullen"),
    Some("Eeveelushop"),
    Role.ADMIN
  )

}
