package com.tamullen.jobsboard.core

import cats.effect.*
import cats.*
import cats.implicits.*
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import com.tamullen.jobsboard.logging.Syntax.*
import org.typelevel.log4cats.Logger
import com.stripe.Stripe as TheStripe
import com.stripe.net.Webhook
import com.tamullen.jobsboard.config.StripeConfig
import scala.util.Try
import scala.jdk.OptionConverters.*

trait Stripe[F[_]] {
  /*
    1. someone calls an endpoint on our server.
        (send a JobInfo to us) - persisted to the DB - Jobs[F].create(...)
    2. return a checkout page URL

    3. Frontend will redirect user to that URL
    4. User pays (fills in CC details....)

    5. backend will be notified by stripe (webhook)
      - test mode: use Stripe CLI to redirect the events to localhost:40401/api/jobs/webhook...
    6. Perform the final operation on the job advert - set the active flag to false for that job id

    app -> http -> Stripe -> redirect user.
                           <- user pays stripe
      activate job <- webhook <- stripe
   */
  def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]]
  def handleWebhookEvent[A](
      payload: String,
      signature: String,
      action: String => F[A]
  ): F[Option[A]]
}

class LiveStripe[F[_]: MonadThrow: Logger](config: StripeConfig) extends Stripe[F] {
  // globally set constant
  TheStripe.apiKey = config.key

  override def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]] =
    SessionCreateParams
      .builder()
      .setMode(SessionCreateParams.Mode.PAYMENT)
      // automatic receipt/invoice
      .setInvoiceCreation(
        SessionCreateParams.InvoiceCreation.builder().setEnabled(true).build()
      )
      .setPaymentIntentData(
        SessionCreateParams.PaymentIntentData
          .builder()
          .setReceiptEmail(userEmail)
          .build()
      )
      .setCustomerEmail(userEmail)
      .setClientReferenceId(jobId)
      .setSuccessUrl(config.successUrl + jobId)
      .setCancelUrl(config.cancelUrl)
      .addLineItem(
        SessionCreateParams.LineItem
          .builder()
          .setQuantity(1L)
          .setPrice(config.price)
          .build()
      )
      .build()
      .pure[F]
      .map(params => Session.create(params))
      .map(_.some)
      .logError(error => s"Creating checkout session failed: $error")
      .recover { case _ => None }

  override def handleWebhookEvent[A](
      payload: String,
      signature: String,
      action: String => F[A]
  ): F[Option[A]] =
    MonadThrow[F]
      .fromTry(
        Try(
          Webhook.constructEvent(
            payload,
            signature,
            config.webhookSecret
          )
        )
      )
      .logError(e => "Stripe security verification failed - possibly fake attempt")
      .flatMap { event =>
        // check event type
        event.getType() match {
          case "checkout.session.completed" => // happy path
            event
              .getDataObjectDeserializer()
              .getObject()                   // Optional[deserializer]
              .toScala                       // Option[deserializer]
              .map(_.asInstanceOf[Session])  // Option[Session]
              .map(_.getClientReferenceId()) // Option[String] <-- stores my job id.
              .map(action)                   // Option[F[A]] == performing the effect
              .sequence                      // F[Option[A]]
              .log(
                {
                  case None =>
                    s"Event ${event.getId()} not producing any effect, check Stripe dashboard.: ${event
                        .getDataObjectDeserializer()
                        .getObject()}" // Optional[deserializer]}"
                  case Some(v) => s"Event ${event.getId()} fully paid -- ok"
                },
                e => s"Webhook action failed: $e"
              )
          case _ =>
            // discard the effect
            None.pure[F]
        }
      }
      .logError(e => s"Something else went wrong: $e")
      .recover { case _ => None }
}

//SessionCreateParams params =
//  SessionCreateParams.builder()
//    .setUiMode(SessionCreateParams.UiMode.EMBEDDED)
//    .setMode(SessionCreateParams.Mode.PAYMENT)
//    .setReturnUrl(YOUR_DOMAIN + "/return.html?session_id={CHECKOUT_SESSION_ID}")
//    .addLineItem(
//      SessionCreateParams.LineItem.builder()
//        .setQuantity(1L)
//        // Provide the exact Price ID (for example, pr_1234) of the product you want to sell
//        .setPrice("{{PRICE_ID}}")
//        .build())
//    .build();
//
//Session session = Session.create(params);

object LiveStripe {
  def apply[F[_]: MonadThrow: Logger](config: StripeConfig): F[LiveStripe[F]] =
    new LiveStripe[F](config).pure[F]
}
