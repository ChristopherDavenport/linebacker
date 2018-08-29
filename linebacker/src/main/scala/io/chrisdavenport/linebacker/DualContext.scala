package io.chrisdavenport.linebacker

import cats._
import cats.effect._
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

trait DualContext[F[_]] extends Linebacker[F] {
  def blockingContext: ExecutionContext
  def contextShift: ContextShift[F]

  def block[A](fa: F[A]): F[A] =
    contextShift.evalOn(blockingContext)(fa)
}

object DualContext {
  def apply[F[_]](implicit ev: DualContext[F]) = ev

  def fromContexts[F[_]: Applicative](
      blocking: ExecutionContext,
      cs: ContextShift[F]): DualContext[F] =
    new DualContext[F] {
      override def blockingContext = blocking
      override def contextShift = cs
    }

  def fromExecutorServices[F[_]: Applicative](
      blocking: ExecutorService,
      cs: ContextShift[F]
  ): DualContext[F] = new DualContext[F] {
    override def blockingContext = ExecutionContext.fromExecutorService(blocking)
    override def contextShift = cs
  }
}
