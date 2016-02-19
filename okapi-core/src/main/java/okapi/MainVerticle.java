/*
 * Copyright (c) 2015-2016, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package okapi;

import okapi.service.ModuleManager;
import opkapi.web.TenantWebService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import okapi.bean.Modules;
import opkapi.web.HealthService;
import okapi.service.ModuleStore;
import opkapi.web.ModuleWebService;
import okapi.service.ProxyService;
import okapi.service.TenantManager;
import okapi.service.TimeStampStore;
import okapi.service.impl.ModuleStoreMemory;
import okapi.service.impl.ModuleStoreMongo;
import okapi.service.impl.MongoHandle;
import okapi.service.impl.TenantStoreMemory;
import okapi.service.impl.TimeStampMemory;
import okapi.service.impl.TimeStampMongo;

public class MainVerticle extends AbstractVerticle {
  private final int port = Integer.parseInt(System.getProperty("port", "9130"));
  private final int port_start = Integer.parseInt(System.getProperty("port_start", Integer.toString(port+1) ));
  private final int port_end = Integer.parseInt(System.getProperty("port_end", Integer.toString(port_start+10)));
  private final String storage = System.getProperty("storage", "mongo");

  HealthService hc;
  ModuleManager ms;
  ModuleWebService moduleWebService;
  ProxyService ps;
  TenantWebService ts;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    hc = new HealthService();

    TenantStoreMemory tenantStore = new TenantStoreMemory();
    TenantManager tman = new TenantManager();
    ts = new TenantWebService(vertx, tman, tenantStore);


    Modules modules = new Modules();
    ms = new ModuleManager(vertx, modules, port_start, port_end);
    ModuleStore moduleStore = null;
    TimeStampStore timeStampStore = null;

    switch (storage) {
      case "mongo":
        MongoHandle mongo = new MongoHandle(vertx);
        moduleStore = new ModuleStoreMongo(mongo);
        timeStampStore = new TimeStampMongo(mongo);
        break;
      case "inmemory":
        moduleStore = new ModuleStoreMemory(vertx);
        timeStampStore = new TimeStampMemory(vertx);
        break;
      default:
        System.out.println("FATAL: Unknown storage type '" + storage + "'");
        System.exit(1);
    }
    moduleWebService = new ModuleWebService(vertx, ms, moduleStore, timeStampStore );
    ps = new ProxyService(vertx, modules, tman);
  }

  public void NotFound(RoutingContext ctx) {
    ctx.response().setStatusCode(404).end("Okapi: unrecognized service");
  }

  @Override
  public void start(Future<Void> fut) throws IOException {
    Router router = Router.router(vertx);
    
    //handle CORS
    router.route().handler(CorsHandler.create("*")
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST));

    //hijack everything to conduit to allow for configuration
    router.route("/_*").handler(BodyHandler.create()); //enable reading body to string
    router.post("/_/modules/").handler(moduleWebService::create);
    router.delete("/_/modules/:id").handler(moduleWebService::delete);
    router.get("/_/modules/:id").handler(moduleWebService::get);
    router.get("/_/modules/").handler(moduleWebService::list);
    router.post("/_/tenants").handler(ts::create);
    router.get("/_/tenants/").handler(ts::list);
    router.get("/_/tenants/:id").handler(ts::get);
    router.delete("/_/tenants/:id").handler(ts::delete);
    router.post("/_/tenants/:id/modules").handler(ts::enableModule);
    router.get("/_/tenants/:id/modules").handler(ts::listModules);
    router.get("/_/health").handler(hc::get);
    router.delete("/_/initmodules").handler(moduleWebService::init);
    router.get("/_/reloadmodules").handler(moduleWebService::reloadModules);

    router.route("/_*").handler(this::NotFound);
    
    //everything else gets proxified to modules
    router.route("/*").handler(ps::proxy);
    
    System.out.println("API Gateway started PID "
      + ManagementFactory.getRuntimeMXBean().getName()
      + ". Listening on port " + port );
    
    vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(port,
                    result -> {
                      if (result.succeeded()) {
                        fut.complete();
                      } else {
                        fut.fail(result.cause());
                      }
                    }
            );
  }

  @Override
  public void stop(Future<Void> fut) throws IOException {
    fut.complete();
  }
}
