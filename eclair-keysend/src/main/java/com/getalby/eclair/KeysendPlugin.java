package com.getalby.eclair;


import akka.actor.ActorRef;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.scaladsl.server.RequestContext;
import akka.http.scaladsl.server.RouteResult;
import fr.acinq.bitcoin.PublicKey;
import fr.acinq.bitcoin.scalacompat.ByteVector32;
import fr.acinq.bitcoin.scalacompat.Crypto;
import fr.acinq.eclair.*;
import fr.acinq.eclair.api.directives.EclairDirectives;
import fr.acinq.eclair.payment.relay.Relayer;
import fr.acinq.eclair.payment.send.PaymentInitiator;
import fr.acinq.eclair.router.Graph.HeuristicsConstants;
import fr.acinq.eclair.router.Graph.WeightRatios;
import fr.acinq.eclair.router.Router;
import fr.acinq.eclair.wire.protocol.GenericTlv;
import scala.Function1;
import scala.Option;
import scala.concurrent.Future;
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.FutureConverters;
import scala.util.Either;
import scala.util.Left;
import scodec.bits.ByteVector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static akka.http.javadsl.server.Directives.*;
import static akka.pattern.Patterns.ask;

public class KeysendPlugin implements Plugin, RouteProvider {

    private Kit kit;
    private ActorRef keySendActor;

    @Override
    public PluginParams params() {
        return () -> "KeysendPlugin";
    }

    @Override
    public void onSetup(Setup setup) {

    }

    @Override
    public void onKit(Kit kit) {
        this.keySendActor = kit.system().actorOf(KeysendActor.props());
        this.kit = kit;
    }

    @Override
    public Function1<RequestContext, Future<RouteResult>> route(EclairDirectives directives) {
        final Router.SearchBoundaries boundaries = new Router.SearchBoundaries(
                new MilliSatoshi(1000L),
                5.0,
                1,
                new CltvExpiryDelta(10)
        );

        final Either<WeightRatios, HeuristicsConstants> heuristics = new Left<>(
                new WeightRatios(1.0, 0.0, 0.0, 0.0, new Relayer.RelayFees(new MilliSatoshi(100), 100))
        );

        final Router.MultiPartParams mpp = new Router.MultiPartParams(new MilliSatoshi(1000L), 2);
        final Router.RouteParams params = new Router.RouteParams(false,
                boundaries,
                heuristics,
                mpp, "", false);

        final var unmarshaller = Jackson.unmarshaller(KeySendBody.class);
        final var alphabet = new Alphabet();

        return path("keysend", () -> post(() -> entity(unmarshaller, body -> {

            final Set<GenericTlv> customTlvs = new HashSet<>();
            body.getCustomTlvs().forEach((tag, value) -> {
                customTlvs.add(new GenericTlv(
                        UInt64.apply(Long.parseLong(tag)),
                        ByteVector.fromValidHex(value, alphabet)
                ));
            });

            final PaymentInitiator.SendSpontaneousPayment ssp = new PaymentInitiator.SendSpontaneousPayment(
                    new MilliSatoshi(body.getAmountMsat()),
                    new Crypto.PublicKey(PublicKey.fromHex(body.getNodeId())),
                    ByteVector32.fromValidHex(generateRandomHexString()),
                    1,
                    Option.apply(""),
                    params,
                    CollectionConverters.asScala(customTlvs).toSeq(),
                    false
            );

            final var future = ask(kit.paymentInitiator(), ssp, 1000L);
            return onComplete(FutureConverters.asJava(future), result -> {
                if (result.isSuccess()) {
                    return complete(result.get().toString());
                } else {
                    return complete("ERROR");
                }
            });
        }))).asScala();
    }

    private String generateRandomHexString() {
        final Random rand = new Random();

        final StringBuilder sb = new StringBuilder();
        while (sb.length() < 64) {
            sb.append(String.format("%08x", rand.nextInt()));
        }

        return sb.substring(0, 64);
    }
}
