package io.singularitynet.sdk.mpe;

import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import io.singularitynet.sdk.ethereum.Address;
import io.singularitynet.sdk.ethereum.WithAddress;
import io.singularitynet.sdk.registry.PaymentGroup;
import io.singularitynet.sdk.registry.PaymentGroupId;

/**
 * Blockchain payment channel manager implementation.
 */
public class MpePaymentChannelManager implements BlockchainPaymentChannelManager {

    private final MultiPartyEscrowContract mpe;
    
    /**
     * Constructor.
     * @param mpe MultiPartyEscrow contract adapter.
     */
    public MpePaymentChannelManager(MultiPartyEscrowContract mpe) {
        this.mpe = mpe;
    }

    @Override
    public Stream<PaymentChannel> getChannelsAccessibleBy(
            PaymentGroupId paymentGroupId, WithAddress identity) {
        return mpe.getChannelOpenEvents()
            // TODO: move paymentGroupId checking into Ethereum filter
            .filter(ch -> ch.getPaymentGroupId().equals(paymentGroupId))
            .filter(ch -> ch.isAccessibleBy(identity))
            .map(ch -> ch.getChannelId())
            .map(id -> mpe.getChannelById(id).get());
    }

    @Override
    public PaymentChannel openPaymentChannel(PaymentGroup paymentGroup,
            WithAddress signer, BigInteger value, BigInteger expiration) {

        PaymentChannel channel = mpe.openChannel(signer.getAddress(),
                paymentGroup.getPaymentDetails().getPaymentAddress(),
                paymentGroup.getPaymentGroupId(), value, expiration);

        return channel;
    }

    @Override
    public PaymentChannel addFundsToChannel(PaymentChannel channel, BigInteger amount) {
        BigInteger valueInc = mpe.channelAddFunds(channel.getChannelId(), amount);
        return channel.toBuilder()
            .setValue(channel.getValue().add(valueInc))
            .build();
    }

    @Override
    public PaymentChannel extendChannel(PaymentChannel channel, BigInteger expiration) {
        BigInteger newExpiration = mpe.channelExtend(channel.getChannelId(), expiration);
        return channel.toBuilder()
            .setExpiration(newExpiration)
            .build();
    }

    @Override
    public PaymentChannel extendAndAddFundsToChannel(PaymentChannel channel,
            BigInteger expiration, BigInteger amount) {
        MultiPartyEscrowContract.ExtendAndAddFundsResponse response = mpe
            .channelExtendAndAddFunds(channel.getChannelId(), expiration, amount);
        return channel.toBuilder()
            .setExpiration(response.expiration)
            .setValue(channel.getValue().add(response.valueIncrement))
            .build();
    }

}
