package io.chrisdavenport

import cats.effect.Async
import scala.concurrent.ExecutionContext

package object linebacker {
  private[linebacker] def dualShift[F[_]: Async, A](
      initialEc: ExecutionContext,
      endEc: ExecutionContext,
      fa: F[A]) =
    Async[F].bracket(Async.shift[F](initialEc))(_ => fa)(_ => Async.shift[F](endEc))

}
