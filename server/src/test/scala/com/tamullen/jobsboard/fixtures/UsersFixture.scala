package com.tamullen.jobsboard.fixtures

import cats.effect.IO
import com.tamullen.jobsboard.core.Users
import com.tamullen.jobsboard.domain.user.*
import com.tamullen.jobsboard.domain.user.Role.*
import com.tamullen.jobsboard.domain.auth._

trait UsersFixture {
  val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == travisEmail) IO.pure(Some(Travis))
      else IO.pure(None)

    override def create(user: User): IO[String] = IO.pure(user.email)

    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))

    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  val Travis = User(
    "travis@test.com",
    "$2a$10$vQdL/KO4Nf5m26EPOyM9jOeVJwbc3n97QQ7sRmeA/BHe7wJeAwP3W", // tamullen
    Some("Travis"),
    Some("Mullen"),
    Some("tamullen"),
    Role.ADMIN
  )

  val travisEmail    = Travis.email
  val travisPassword = "tamullen"

  val Amber = User(
    "amber@test.com",
    "$2a$10$7wVKg4nQ/ZsWUR4XmBy1VOY2rAX0dojNpony6SkerCpeHe/nXkTQq", // amanley
    Some("Amber"),
    Some("Manley"),
    Some("tamullen"),
    Role.RECRUITER
  )

  val amberEmail    = Amber.email
  val amberPassword = "amanley"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$e46GmU3fTNkwhUncC4KhVO244fjb7sWHc8bk0HOdNOZvZ.DxaWhYy", // simplepassword
    Some("New"),
    Some("User"),
    Some("test company"),
    RECRUITER
  )

  val UpdatedTravis = User(
    "travis@test.com",
    "$2a$10$i8KpMuvwVLBeO8tKYK3NFulfawv.QyGRa98Mi4qt/.nsw42Ui0QIq", // test2
    Some("Travis"),
    Some("Mullen"),
    Some("Eeveelushop"),
    Role.ADMIN
  )

  val NewUserTravis = NewUserInfo(
    travisEmail,
    travisPassword,
    Some("Travis"),
    Some("Mullen"),
    Some("tamullen")
  )

  val NewUserAmber = NewUserInfo(
    amberEmail,
    amberPassword,
    Some("Amber"),
    Some("Manley"),
    Some("tamullen")
  )

}
