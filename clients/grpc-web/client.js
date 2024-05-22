const {RequestId} = require('./out/service-clustering_pb');
const {ClusteringWalletServiceClient} = require('./out/service-clustering_grpc_web_pb');

const service = new ClusteringWalletServiceClient("http://localhost:8200");

service.getBalance(new RequestId("123"), {"header_key": "yoyoyo"}, function(err, response) {
   console.log(response);
});
