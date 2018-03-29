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
  val select: K => F[ExecutionContext]
  def pass[A](fa: F[A], to: K)(implicit F: Async[F]): F[A] =
    for {
      ec <- select(to)
      _ <- Async.shift(ec)
      a <- fa
    } yield a
  def fleaFlicker[A](fa: F[A], initial: K, end: K)(implicit F: Async[F]): F[A] =
    for {
      iEC <- select(initial)
      _ <- Async.shift(iEC)
      aE <- fa.attempt
      eEC <- select(end)
      _ <- Async.shift(eEC)
      a <- Applicative[F].pure(aE).rethrow
    } yield a
}
object Quarterback {
  def apply[F[_], K](implicit ev: Quarterback[F, K]) = ev
}
