package com.tamullen.jobsboard.playground

import cats.effect.{IO, IOApp}
import com.tamullen.jobsboard.config.EmailServiceConfig
import com.tamullen.jobsboard.core.LiveEmails

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

object EmailsPlayground {
  def main(args: Array[String]): Unit = {
    //configs
    //user, pass, host, port
    /*
    Host	smtp.ethereal.email
    Port	587
    Security	STARTTLS
    Username	kathryne.hand75@ethereal.email
    Password	YjEVEzxTrBR16Qa5Kn
     */
    val host = "smtp.ethereal.email"
    val port = 587
    val user = "kathryne.hand75@ethereal.email"
    val pass = "YjEVEzxTrBR16Qa5Kn"
    val frontendUrl = "https://google.com"

    val token = "ABCD1234"
    // properties file
    /*
      mail.smtp.auth = true
      mail.smtp.starttls.enable = true
      mail.smtp.host = host
      mail.smtp.port = port
      mail.smtp.ssl.trust = host
     */
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", true)
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)

    // authentication
    val auth = new Authenticator {
      override def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }

    // session
    val session = Session.getInstance(prop, auth)

    // email itself
    val subject = "Email from tamullen"
    val content =
      s"""
        |<div style="
        | border: 1px solid black;
      |   padding: 20px;
      |   font-family: sans-serif;
      |   line-height: 2;
      |   font-size: 20px;
      |  ">
        | <h1> Rock the jvm: Password Recovery</h1>
        | <p>kiss from tamullen</p>
        | <p>Your password recovery token is: $token</p>
        | <p>
        |   Click <a href="$frontendUrl/login">here</a> to get back to the application.
        | </p>
        |</div>""".stripMargin

    // message = MIME message
    val message = new MimeMessage(session)
    message.setFrom("tamullen@test.com")
    message.setRecipients(Message.RecipientType.TO, "the.user@gmail.com")
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    // send
    Transport.send(message)
  }
}

object EmailsEffectPlayground extends IOApp.Simple {
  override def run: IO[Unit] = for {
    emails <- LiveEmails[IO](
      EmailServiceConfig(host = "smtp.ethereal.email",
        port = 587,
        user = "kathryne.hand75@ethereal.email",
        pass = "YjEVEzxTrBR16Qa5Kn",
        frontendUrl = "https://google.com"),
    )
    _ <- emails.sendPasswordRecoveryEmail("someone@rockthejvm.com", "ROCKTJVM")
  } yield ()
}
