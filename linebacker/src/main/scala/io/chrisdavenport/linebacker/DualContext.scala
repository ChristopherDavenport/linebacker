package io.chrisdavenport.linebacker

import cats._
import cats.effect._
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

trait DualContext[F[_]] {
  def blockingContext: ExecutionContext
  def defaultContext: ExecutionContext

  def block[A](fa: F[A])(implicit F: Async[F]): F[A] =
    dualShift(blockingContext, defaultContext, fa)
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

  def fromExecutorServices[F[_]: Applicative](
      default: ExecutorService,
      blocking: ExecutorService
  ): DualContext[F] = new DualContext[F] {
    override def defaultContext = ExecutionContext.fromExecutorService(default)
    override def blockingContext = ExecutionContext.fromExecutorService(blocking)
  }
}
