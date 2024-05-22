package event_sourcing
package examples

import akka.cluster.ClusterEvent.*
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.stream.scaladsl.Balance as _

object ClusterInfrastructure:

    object ClusterStateChanges:

        def apply(): Behavior[MemberEvent] = Behaviors.setup:
            ctx =>
                Behaviors.receiveMessage:
                    case MemberJoined(member: Member) =>
                      ctx.log.info("MemberJoined: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberWeaklyUp(member: Member) =>
                      ctx.log.info("MemberWeaklyUp: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberUp(member: Member) =>
                      ctx.log.info("MemberUp: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberLeft(member: Member) =>
                      ctx.log.info("MemberLeft: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberPreparingForShutdown(member: Member) =>
                      ctx.log.info("MemberPreparingForShutdown: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberReadyForShutdown(member: Member) =>
                      ctx.log.info("MemberReadyForShutdown: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberExited(member: Member) =>
                      ctx.log.info("MemberExited: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberDowned(member: Member) =>
                      ctx.log.info("MemberDowned: {}", member.uniqueAddress)
                      Behaviors.same

                    case MemberRemoved(member: Member, previousStatus: MemberStatus) =>
                      ctx.log.info2(
                        "MemberRemoved: {}, previousStatus: {}",
                        member.uniqueAddress,
                        previousStatus
                      )
                      Behaviors.same
