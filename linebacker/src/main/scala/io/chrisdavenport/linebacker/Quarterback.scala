package io.chrisdavenport.linebacker

import cats._
import cats.effect._
import cats.implicits._
import scala.concurrent.ExecutionContext

/**
 * This is the thread pool manager.
 * Select ExecutionContexts whenever you would like.
 * Execute specific actions on specific threadpools.
 * Options relevant to a decision are available to you.
 *
 */
trait Quarterback[F[_], K] {
  val select: K => F[ExecutionContext]
  def pass[A](fa: F[A], to: K)(implicit F: FlatMap[F], CS: ContextShift[F]): F[A] =
    for {
      ec <- select(to)
      a <- CS.evalOn(ec)(fa)
    } yield a
}
object Quarterback {
  def apply[F[_], K](implicit ev: Quarterback[F, K]) = ev
}
