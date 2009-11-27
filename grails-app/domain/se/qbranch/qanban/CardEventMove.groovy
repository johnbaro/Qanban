package se.qbranch.qanban

class CardEventMove extends Event implements Comparable {

    static constraints = {

    }

    static transients = ['card']

    Integer newCardIndex
    Card card

    Phase newPhase
    //Date dateCreated

    transient beforeInsert = {
        domainId = card.domainId
    }

    transient afterInsert = {

        card.phase.cards.remove(card)
        newPhase.cards.add(newCardIndex, card)
        card.phase = newPhase
        card.save()
        
    }


    int compareTo(Object o) {
        if (o instanceof Event) {
            Event event = (Event) o
            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if(this.dateCreated < event.dateCreated) return AFTER
            if(this.dateCreated > event.dateCreated) return BEFORE

            return EQUAL
        }
    }

    boolean equals(Object o) {
        if(o instanceof Event) {
            Event event = (Event) o
            if(this.id == event.id)
            return true
        }
        return false
    }
}