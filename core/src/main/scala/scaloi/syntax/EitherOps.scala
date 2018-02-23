package scaloi
package syntax

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Enhancements on eithers.
  *
  * @param self the either
  * @tparam A the left type
  * @tparam B the right type
  */
final class EitherOps[A, B](val self: Either[A, B]) extends AnyVal {
  import AnyOps._

  /**
    * Left tap: apply a kestrel combinator to the left value of a either.
    * Runs a side-effect on the left value and returns this either unchanged.
    *
    * @param f the function to apply to the left value
    * @tparam T the result type of the function
    * @return the left value
    */
  def leftTap[T](f: A => T): Either[A, B] = leftMap {
    _ <| f
  }

  /**
    * An alias for leftTap.
    */
  @inline final def -<|[T](f: A => T): Either[A, B] = leftTap(f)

  /**
    * Right tap: apply a kestrel combinator to the right value of a either.
    * Runs a side-effect on the right value and returns this either unchanged.
    *
    * @param f the function to apply to the right value
    * @tparam T the result type of the function
    * @return the right value
    */
  def rightTap[T](f: B => T): Either[A, B] = self map {
    _ <| f
  }

  /**
    * An alias for rightTap.
    */
  @inline final def <|-[T](f: B => T): Either[A, B] = rightTap(f)

  /**
    * Map over the left value.
    */
  @inline final def leftMap[C](f: A => C): Either[C, B] = self.left.map(f)

  /**
    * Get the right value or throw the throwable from the left value.
    *
    * @param ev evidence for the throwable of the left type
    * @return the right value
    */
  def orThrow(implicit ev: A <:< Throwable): B = valueOr(a => throw ev(a))

  /**
    * Get the right value of the either or run the given function on the left.
    * @param x the function to apply to the left
    * @tparam BB the result type
    * @return either the right value or the transformed left
    */
  def valueOr[BB >: B](x: A => BB): BB =
    self match {
      case Left(a) => x(a)
      case Right(b) => b
    }

  /**
    * Convert this to a `Try` if the left has a throwable.
    *
    * @param ev evidence for the throwable of the left type
    * @return the resulting `Try`
    */
  def toTry(implicit ev: A <:< Throwable): Try[B] = self.fold(e => Failure(ev(e)), Success.apply)
}

/**
  * Either operations companion.
  */
object EitherOps extends ToEitherOps {
}

/**
  * Implicit conversion for either operations.
  */
trait ToEitherOps {

  /**
    * Implicit conversion from either to the either enhancements.
    * @param d the either value
    * @tparam A the left type
    * @tparam B the right type
    */
  implicit def toEitherOps[A, B](d: Either[A, B]): EitherOps[A, B] = new EitherOps(d)
}