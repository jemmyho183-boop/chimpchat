package com.chimpchat.api.automation.business;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InstagramCommentAutomationListener {
    private final InstagramCommentAutomationBusiness automationBusiness;

    public InstagramCommentAutomationListener(InstagramCommentAutomationBusiness automationBusiness) {
        this.automationBusiness = automationBusiness;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(InstagramCommentReceivedEvent event) {
        automationBusiness.processComment(event.commentId());
    }
}
