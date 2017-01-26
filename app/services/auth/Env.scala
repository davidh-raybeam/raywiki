package services.auth

import models.User

import com.mohiva.play.silhouette
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator

trait Env extends silhouette.api.Env {
  type I = User
  type A = SessionAuthenticator
}
