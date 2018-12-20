package utils

import javax.inject.{Inject, Singleton}
import play.api.libs.mailer.{Email, MailerClient}

/**
  * @author dave.th
  * @date 15/11/2018
  */
@Singleton
trait SendMail {
  def sendMail(to: Seq[String], subject: String, message: String)
}
class MailUtils @Inject()(mailerClient: MailerClient) extends SendMail {

  override def sendMail(to: Seq[String], subject: String, message: String) = {
    val email = Email(
      from = s"우리오늘뭐해? <dkt.de.todo@gmail.com>",
      to = to,
      subject = subject,
      bodyHtml = Some(message)
    )
    mailerClient.send(email)
  }
}
