package infrastructure
package persistence

import akka.persistence.typed.EventAdapter
import akka.persistence.typed.EventSeq
import com.journal.account.events as EventsDataModel

import persistence.WalletEvents as DomainEvents

// import WalletEvents as DomainEvents // this breaks the formatter

class AccountDetachedModelsAdapter extends EventAdapter[DomainEvents.Event, EventsDataModel.Event]:

    override def manifest(event: DomainEvents.Event): String = ""

    override def toJournal(event: DomainEvents.Event): EventsDataModel.Event = ???
    // event match
    //   case DomainEvents.WalletCreated()    => EventsDataModel.WalletCreated()
    //   case DomainEvents.CreditAdded(value) =>
    //     val nM = EventsDataModel.NewAmount(value, "lalalal")
    //     EventsDataModel.CreditAdded(None, Some(nM))
    //   // EventsDataModel.CreditAdded(value)

    //   case DomainEvents.DebitAdded(value) => EventsDataModel.DebitAdded(value)

    override def fromJournal(event: EventsDataModel.Event, manifest: String): EventSeq[DomainEvents.Event] = ???
    // event match
    //   case EventsDataModel.WalletCreated() => EventSeq.single(DomainEvents.WalletCreated())

    //   case EventsDataModel.CreditAdded(Some(value), _) => EventSeq.single(DomainEvents.CreditAdded(value))

    //   case EventsDataModel.CreditAdded(_, Some(value)) => EventSeq.single(DomainEvents.CreditAdded(value.amount))

    //   case EventsDataModel.DebitAdded(value) => EventSeq.single(DomainEvents.DebitAdded(value))
