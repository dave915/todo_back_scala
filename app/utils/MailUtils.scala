package utils

import javax.inject.Inject
import play.api.libs.mailer.{Email, MailerClient}

/**
  * @author dave.th
  * @date 15/11/2018
  */
trait SendMail {
  def sendMail(to: String, subject: String, message: String)
}
class MailUtils @Inject()(mailerClient: MailerClient) extends SendMail {

  override def sendMail(to: String, subject: String, message: String) = {
    val email = Email(
      from = s"우리오늘뭐해? <dkt.de.todo@gmail.com>",
      to = Seq(to),
      subject = subject,
      bodyHtml = Some(message)
    )
    mailerClient.send(email)
  }
}
