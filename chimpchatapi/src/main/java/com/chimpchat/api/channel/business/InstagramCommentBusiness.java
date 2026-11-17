package com.chimpchat.api.channel.business;

import java.util.List;

import com.chimpchat.api.automation.business.InstagramCommentReceivedEvent;
import com.chimpchat.api.channel.entity.ChannelConnectionEntity;
import com.chimpchat.api.channel.entity.InstagramCommentEntity;
import com.chimpchat.api.channel.repository.ChannelConnectionRepository;
import com.chimpchat.api.channel.repository.InstagramCommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstagramCommentBusiness {
    private static final Logger logger = LoggerFactory.getLogger(InstagramCommentBusiness.class);
    private static final String PROVIDER = "INSTAGRAM";

    private final ChannelConnectionRepository connectionRepository;
    private final InstagramCommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InstagramCommentBusiness(
            ChannelConnectionRepository connectionRepository,
            InstagramCommentRepository commentRepository,
            ApplicationEventPublisher eventPublisher) {
        this.connectionRepository = connectionRepository;
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void receiveComment(
            String instagramAccountId,
            String externalCommentId,
            String externalMediaId,
            String parentCommentId,
            String commenterId,
            String commenterUsername,
            String commentText,
            String mediaProductType,
            String commentVerb) {
        if (parentCommentId != null || instagramAccountId.equals(commenterId)) {
            logger.debug("Ignoring an Instagram comment reply or a comment authored by the connected account.");
            return;
        }

        List<ChannelConnectionEntity> connections =
                connectionRepository.findAllWithAccountByProviderAndExternalAccountId(
                        PROVIDER, instagramAccountId);
        if (connections.isEmpty()) {
            logger.warn("Received a comment for an Instagram account not connected to ChimpChat.");
            return;
        }

        for (ChannelConnectionEntity connection : connections) {
            String accountUsername = connection.getAccounts().stream()
                    .filter(account -> instagramAccountId.equals(account.getExternalAccountId()))
                    .map(account -> account.getUsername())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Instagram account disappeared while processing its comment."));
            var existingComment = commentRepository.findByConnectionIdAndInstagramAccountIdAndExternalCommentId(
                    connection.getId(), instagramAccountId, externalCommentId);
            boolean isNewComment = existingComment.isEmpty();
            InstagramCommentEntity comment = existingComment.orElseGet(() -> new InstagramCommentEntity(
                    connection,
                    instagramAccountId,
                    accountUsername,
                    externalCommentId,
                    externalMediaId,
                    commenterId,
                    commenterUsername,
                    commentText,
                    mediaProductType,
                    commentVerb));
            if (existingComment.isPresent()) {
                comment.updateFromWebhook(
                        accountUsername,
                        externalMediaId,
                        commenterId,
                        commenterUsername,
                        commentText,
                        mediaProductType,
                        commentVerb);
            }
            if (isNewComment) {
                commentRepository.saveAndFlush(comment);
                eventPublisher.publishEvent(new InstagramCommentReceivedEvent(comment.getId()));
            } else {
                commentRepository.save(comment);
            }
        }
    }

}
