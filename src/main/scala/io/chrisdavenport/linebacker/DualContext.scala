package io.chrisdavenport.linebacker

import cats._
import cats.implicits._
import cats.effect._
import scala.concurrent.ExecutionContext

trait DualContext[F[_]] {
  def blockingContext: ExecutionContext
  def defaultContext: ExecutionContext

  def block[A](fa: F[A])(implicit F: Async[F]): F[A] =
    for {
      _ <- Async.shift(blockingContext)
      aE <- fa.attempt
      _ <- Async.shift(defaultContext)
      a <- Applicative[F].pure(aE).rethrow
    } yield a
}

object DualContext {
  def apply[F[_]](implicit ev: DualContext[F]) = ev

  def fromContexts[F[_]: Applicative](
      default: ExecutionContext,
      blocking: ExecutionContext): DualContext[F] =
    new DualContext[F] {
      override def blockingContext = blocking
      override def defaultContext = default
    }
}
