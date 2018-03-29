package io.chrisdavenport.linebacker

import cats._
import cats.effect.Async
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
  val select: K => ExecutionContext
  def pass[A](fa: F[A], to: K)(implicit F: Async[F]): F[A] =
    Async.shift(select(to)) *> fa
  def fleaFlicker[A](fa: F[A], initial: K, end: K)(implicit F: Async[F]): F[A] =
    for {
      _ <- Async.shift(select(initial))
      aE <- fa.attempt
      _ <- Async.shift(select(end))
      a <- Applicative[F].pure(aE).rethrow
    } yield a
}
