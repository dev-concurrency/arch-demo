<script setup>
import { ref } from 'vue'

import { createPromiseClient } from "@connectrpc/connect"
import { createGrpcWebTransport } from "@connectrpc/connect-web"
import { ClusteringWalletService } from "../connect-web-gen/service-clustering_connect"
import {RequestId, Response} from "../connect-web-gen/service-clustering_pb"

// const props = defineProps({
//   msg: String,
// })

const message = ref("")


const transport = createGrpcWebTransport({
  baseUrl: "http://localhost:8200",
});

const client = createPromiseClient(ClusteringWalletService, transport)

function send_message() {
  client.getBalance(new RequestId({"id":"123"}), {headers: {"header_key": "yoyoyo"}}).then((response) => {
    console.log(response)
    message.value = response.balance
  })
}

</script>

<template>
  <h1>Template</h1>

  <div class="card">
    <button type="button" @click="send_message">Call</button>
    <p>
      Response value: {{ message }}
    </p>
  </div>

</template>

<style scoped>
.read-the-docs {
  color: #888;
}
</style>
