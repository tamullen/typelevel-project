package com.tamullen.jobsboard.playground

import cats.effect.*
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers._

object PasswordHashingPlayground extends IOApp.Simple {
  override def run: IO[Unit] =
    BCrypt.hashpw[IO]("tamullen").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("amanley").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("simplepassword").flatMap(IO.println) *>
      BCrypt.hashpw[IO]("test2").flatMap(IO.println)

}
