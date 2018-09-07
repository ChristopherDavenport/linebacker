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

  def fromContextShift[F[_]: Applicative](
      cs: ContextShift[F],
      blocking: ExecutionContext): DualContext[F] =
    new DualContext[F] {
      override def blockingContext = blocking
      override def contextShift = cs
    }

  def fromContexts[F[_]: Applicative](
      default: ExecutionContext,
      blocking: ExecutionContext): DualContext[F] = {
    val contextShift =
      new ContextShift[F] {
        def shift: F[Unit] = Async.shift[F](default)

        def evalOn[A](blocking: ExecutionContext)(f: F[A]): F[A] =
          dualShift(blocking, default, f)
      }

    fromContextShift(contextShift, blocking)
  }

  def fromExecutorServices[F[_]: Applicative](
      default: ExecutorService,
      blocking: ExecutorService): DualContext[F] =
    fromContexts(ExecutionContext.fromExecutor(default), ExecutionContext.fromExecutor(blocking))
}
