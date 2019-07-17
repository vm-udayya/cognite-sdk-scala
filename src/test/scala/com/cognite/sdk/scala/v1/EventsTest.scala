package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}

class EventsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.events)

  it should behave like readableWithRetrieve(client.events, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like writable(
    client.events,
    Seq(Event(description = Some("scala-sdk-read-example-1")), Event(description = Some("scala-sdk-read-example-2"))),
    Seq(CreateEvent(description = Some("scala-sdk-create-example-1")), CreateEvent(description = Some("scala-sdk-create-example-2"))),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  private val eventsToCreate = Seq(
    Event(description = Some("scala-sdk-update-1"), `type` = Some("test"), subtype = Some("test")),
    Event(description = Some("scala-sdk-update-2"), `type` = Some("test"), subtype = Some("test"))
  )
  private val eventUpdates = Seq(
    Event(description = Some("scala-sdk-update-1-1"), `type` = Some("testA"), subtype = Some(null)), // scalastyle:ignore null
    Event(
      description = Some("scala-sdk-update-2-1"),
      `type` = Some("testA"),
      subtype = Some("test-1")
    )
  )

  it should behave like updatable(
    client.events,
    eventsToCreate,
    eventUpdates,
    (id: Long, item: Event) => item.copy(id = id),
    (a: Event, b: Event) => { a == b },
    (readEvents: Seq[Event], updatedEvents: Seq[Event]) => {
      assert(eventsToCreate.size == eventUpdates.size)
      assert(readEvents.size == eventsToCreate.size)
      assert(readEvents.size == updatedEvents.size)
      assert(updatedEvents.zip(readEvents).forall { case (updated, read) =>
        updated.description.nonEmpty &&
          read.description.nonEmpty &&
          updated.description.forall { description => description == s"${read.description.get}-1"}
      })
      assert(readEvents.head.subtype.isDefined)
      assert(updatedEvents.head.subtype.isEmpty)
      assert(updatedEvents(1).subtype == eventUpdates(1).subtype)
      ()
    }
  )

  it should "support search" in {
    val createdTimeSearchResults = client.events
      .search(
        EventsQuery(
          filter = Some(EventsFilter(createdTime = Some(TimeRange(1541510008838L, 1541515508838L))))
        )
      )
      .unsafeBody
    assert(createdTimeSearchResults.length == 11)
    val subtypeCreatedTimeSearchResults = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime = Some(TimeRange(1541510008838L, 1541515508838L)),
              `type` = Some("Workorder"),
              subtype = Some("Foo")
            )
          )
        )
      )
      .unsafeBody
    assert(subtypeCreatedTimeSearchResults.length == 1)
    val searchResults = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime = Some(TimeRange(0L, 1541515508838L))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("description")
            )
          )
        )
      )
      .unsafeBody
    assert(searchResults.length == 3)
    val searchResults2 = client.events
      .search(
        EventsQuery(
          filter = Some(
            EventsFilter(
              createdTime = Some(TimeRange(0L, 1552395929193L))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("description")
            )
          )
        )
      )
      .unsafeBody
    assert(searchResults2.length == 7)
    val limitSearchResults = client.events
      .search(
        EventsQuery(
          limit = 3,
          filter = Some(
            EventsFilter(
              createdTime = Some(TimeRange(0L, 1552395929193L))
            )
          ),
          search = Some(
            EventsSearch(
              description = Some("description")
            )
          )
        )
      )
      .unsafeBody
    assert(limitSearchResults.length == 3)
  }
}
