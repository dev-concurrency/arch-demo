/// This file has messages for describing a customer.

// @generated by protoc-gen-connect-es v1.4.0 with parameter "target=ts"
// @generated from file service-clustering.proto (package com.wallet.demo.clustering.grpc.admin, syntax proto3)
/* eslint-disable */
// @ts-nocheck

import { BalanceResponse, CreditRequest, DebitRequest, RequestId, Response, rpcOperationRequest, rpcOperationResponse } from "./service-clustering_pb.js";
import { MethodKind } from "@bufbuild/protobuf";

/**
 * @generated from service com.wallet.demo.clustering.grpc.admin.ClusteringWalletService
 */
export const ClusteringWalletService = {
  typeName: "com.wallet.demo.clustering.grpc.admin.ClusteringWalletService",
  methods: {
    /**
     * @generated from rpc com.wallet.demo.clustering.grpc.admin.ClusteringWalletService.operation
     */
    operation: {
      name: "operation",
      I: rpcOperationRequest,
      O: rpcOperationResponse,
      kind: MethodKind.Unary,
    },
    /**
     * @generated from rpc com.wallet.demo.clustering.grpc.admin.ClusteringWalletService.createWallet
     */
    createWallet: {
      name: "createWallet",
      I: RequestId,
      O: Response,
      kind: MethodKind.Unary,
    },
    /**
     * @generated from rpc com.wallet.demo.clustering.grpc.admin.ClusteringWalletService.deleteWallet
     */
    deleteWallet: {
      name: "deleteWallet",
      I: RequestId,
      O: Response,
      kind: MethodKind.Unary,
    },
    /**
     * @generated from rpc com.wallet.demo.clustering.grpc.admin.ClusteringWalletService.addCredit
     */
    addCredit: {
      name: "addCredit",
      I: CreditRequest,
      O: Response,
      kind: MethodKind.Unary,
    },
    /**
     * @generated from rpc com.wallet.demo.clustering.grpc.admin.ClusteringWalletService.addDebit
     */
    addDebit: {
      name: "addDebit",
      I: DebitRequest,
      O: Response,
      kind: MethodKind.Unary,
    },
    /**
     * @generated from rpc com.wallet.demo.clustering.grpc.admin.ClusteringWalletService.getBalance
     */
    getBalance: {
      name: "getBalance",
      I: RequestId,
      O: BalanceResponse,
      kind: MethodKind.Unary,
    },
  }
} as const;
