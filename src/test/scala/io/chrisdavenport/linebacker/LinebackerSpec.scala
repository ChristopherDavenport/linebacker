package io.chrisdavenport.linebacker

import org.specs2._
import cats.effect.IO
import fs2.Stream
import java.lang.Thread

class LinebackerSpec extends Spec {
  override def is = s2"""
  Threads Run On Linebacker $runsOnLinebacker
  """

  def runsOnLinebacker = {
    Linebacker
      .stream[IO]
      .flatMap { implicit linebacker =>
        import scala.concurrent.ExecutionContext.Implicits.global
        Stream.eval(
          Linebacker[IO].block(IO(Thread.currentThread().getName))
        )
      }
      .compile
      .last
      .unsafeRunSync must_== Some("linebacker-thread-0")
  }
}
