package io.chrisdavenport.linebacker

import cats._
import cats.implicits._
import cats.effect._
import fs2.Stream
import scala.concurrent.ExecutionContext

trait DualContext[F[_]] {
  def blockingContext: F[ExecutionContext]
  def defaultContext: F[ExecutionContext]

  def block[A](fa: F[A])(implicit F: Async[F]): F[A] =
    for {
      bEC <- blockingContext
      dEC <- defaultContext
      _ <- Async.shift(bEC)
      aE <- fa.attempt
      _ <- Async.shift(dEC)
      a <- Applicative[F].pure(aE).rethrow
    } yield a
}

object DualContext {
  def apply[F[_]](implicit ev: DualContext[F]) = ev

  def fromContexts[F[_]: Applicative](
      default: ExecutionContext,
      blocking: ExecutionContext): DualContext[F] =
    new DualContext[F] {
      override def blockingContext = Applicative[F].pure(blocking)
      override def defaultContext = Applicative[F].pure(default)
    }

  def fromLinebacker[F[_]: Sync](implicit EC: ExecutionContext): Stream[F, DualContext[F]] =
    for {
      lb <- Linebacker.stream[F]
    } yield
      new DualContext[F] {
        override def blockingContext = Applicative[F].pure(lb.blockingPool)
        override def defaultContext = Applicative[F].pure(EC)
      }
}
