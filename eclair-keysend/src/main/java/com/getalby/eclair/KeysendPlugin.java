package com.getalby.eclair;


import akka.actor.ActorRef;
import akka.http.scaladsl.server.RequestContext;
import akka.http.scaladsl.server.RouteResult;
import fr.acinq.bitcoin.PublicKey;
import fr.acinq.bitcoin.scalacompat.ByteVector32;
import fr.acinq.bitcoin.scalacompat.Crypto;
import fr.acinq.eclair.*;
import fr.acinq.eclair.api.directives.EclairDirectives;
import fr.acinq.eclair.payment.relay.Relayer;
import fr.acinq.eclair.payment.send.PaymentInitiator;
import fr.acinq.eclair.router.Graph;
import fr.acinq.eclair.router.Graph.HeuristicsConstants;
import fr.acinq.eclair.router.Graph.WeightRatios;
import fr.acinq.eclair.router.Router;
import fr.acinq.eclair.wire.protocol.GenericTlv;
import scala.Function1;
import scala.Option;
import scala.collection.immutable.Set;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import scala.util.Left;
import scodec.bits.ByteVector;

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
        Router.SearchBoundaries boundaries = new Router.SearchBoundaries(new MilliSatoshi(1000L), 5.0, 1, new CltvExpiryDelta(10));
        scala.util.Either<WeightRatios, HeuristicsConstants> heuristics = new Left<>(new Graph.WeightRatios(1.0, 0.0, 0.0, 0.0, new Relayer.RelayFees(new MilliSatoshi(100), 100)));
        Router.MultiPartParams mpp = new Router.MultiPartParams(new MilliSatoshi(1000L), 2);
        Router.RouteParams params = new Router.RouteParams(false,
                boundaries,
                heuristics,
                mpp, "", false);
        PaymentInitiator.SendSpontaneousPayment ssp = new PaymentInitiator.SendSpontaneousPayment(
                new MilliSatoshi(1000),
                new Crypto.PublicKey(PublicKey.fromHex("037e9d070515c8ba32e6a32fa698e568fc4944a1bb67ae2048947511267202509b")),
                ByteVector32.One(),
                1,
                Option.apply(""),
                params,
                new Set.Set1<GenericTlv>(new GenericTlv(UInt64.apply(1), ByteVector.empty())).toSeq(),
                false);
        final var fut = ask(kit.paymentInitiator(), ssp, 1000L);
        final var s = FutureConverters.toJava(fut);
        return path("keysend", () ->
                onComplete(s, res -> complete((String) res.get().toString()))).asScala();

    }
}
