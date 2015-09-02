package ru.codeunited.wmq.bus;

import ru.codeunited.wmq.messaging.MQLink;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 19.05.15.
 */
public class QMDisconnectedEvent implements QMCommunicationEvent {

    private MQLink link;

    public QMDisconnectedEvent(MQLink link) {
        this.link = link;
    }

    @Override
    public MQLink getLink() {
        return link;
    }
}
