// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

import java.time.Instant

import com.cognite.sdk.scala.v1.{
  CogniteExternalId,
  CogniteId,
  CogniteInternalId,
  ContainsAll,
  ContainsAny,
  LabelContainsFilter,
  LabelsOnUpdate,
  Sequence,
  SequenceColumn,
  SequenceColumnCreate,
  SequenceCreate,
  TimeRange
}
import io.circe.{Decoder, Encoder, Json}
import io.circe.derivation.deriveEncoder
import io.scalaland.chimney.Transformer

package object common {
  implicit val cogniteIdEncoder: Encoder[CogniteId] = new Encoder[CogniteId] {
    final def apply(i: CogniteId): Json = Json.obj(
      i match {
        case i: CogniteInternalId => ("id", Json.fromLong(i.id))
        case e: CogniteExternalId => ("externalId", Json.fromString(e.externalId))
      }
    )
  }
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder
  implicit val cogniteInternalIdEncoder: Encoder[CogniteInternalId] = deriveEncoder
  implicit val cogniteInternalIdItemsEncoder: Encoder[Items[CogniteInternalId]] = deriveEncoder
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val cogniteExternalIdItemsEncoder: Encoder[Items[CogniteExternalId]] = deriveEncoder

  implicit val labelContainsEncoder: Encoder[LabelContainsFilter] =
    new Encoder[LabelContainsFilter] {
      final def apply(i: LabelContainsFilter): Json = Json.obj(
        i match {
          case ca: ContainsAny =>
            (
              "containsAny",
              Json.arr(
                ca.containsAny.map(s => Json.obj(("externalId", Json.fromString(s.externalId)))): _*
              )
            )
          case conAll: ContainsAll =>
            (
              "containsAll",
              Json.arr(
                conAll.containsAll
                  .map(s => Json.obj(("externalId", Json.fromString(s.externalId)))): _*
              )
            )
        }
      )
    }
  implicit val containsAnyEncoder: Encoder[ContainsAny] = deriveEncoder
  implicit val containsAllEncoder: Encoder[ContainsAll] = deriveEncoder

  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)
  implicit val timeRangeEncoder: Encoder[TimeRange] = deriveEncoder

  implicit val deleteRequestWithIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithIgnoreUnknownIds[CogniteId]] =
    deriveEncoder

  // externalId optional when reading, despite being required when
  // writing. This is due to it being optional in v0.6, and data from
  // there has not yet been migrated.
  // When it has, and externalId has been marked as required in the
  // official API docs, we should be able to remove this.
  implicit val sequenceColumnToCreateTransformer
      : Transformer[SequenceColumn, SequenceColumnCreate] =
    Transformer
      .define[SequenceColumn, SequenceColumnCreate]
      .withFieldComputed(_.externalId, r => r.externalId.getOrElse(""))
      .buildTransformer

  implicit val sequenceToCreateTransformer: Transformer[Sequence, SequenceCreate] =
    Transformer.define[Sequence, SequenceCreate].buildTransformer

  implicit val strSeqToLabelsOnUpdateTransformer
      : Transformer[Option[Seq[String]], Option[LabelsOnUpdate]] =
    new Transformer[Option[Seq[String]], Option[LabelsOnUpdate]] {
      override def transform(src: Option[Seq[String]]) = src match {
        case Some(value: Seq[String]) =>
          Some(LabelsOnUpdate(add = Some(value.map(CogniteExternalId))))
        case _ => None
      }
    }

  implicit val extIdSeqToLabelsOnUpdateTransformer
      : Transformer[Option[Seq[CogniteExternalId]], Option[LabelsOnUpdate]] =
    new Transformer[Option[Seq[CogniteExternalId]], Option[LabelsOnUpdate]] {
      override def transform(src: Option[Seq[CogniteExternalId]]) = src match {
        case Some(value: Seq[CogniteExternalId]) => Some(LabelsOnUpdate(add = Some(value)))
        case _ => None
      }
    }

  implicit val labelsOnUpdateEncoder: Encoder[LabelsOnUpdate] = deriveEncoder
}
