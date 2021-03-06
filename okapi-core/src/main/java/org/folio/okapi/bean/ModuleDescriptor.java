package org.folio.okapi.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Description of a module. These are used when creating modules under
 * "/_/proxy/modules" etc.
 *
 */
@JsonInclude(Include.NON_NULL)
public class ModuleDescriptor {
  private final Logger logger = LoggerFactory.getLogger("okapi");

  private String id;
  private String name;

  private String[] tags;
  private EnvEntry[] env;

  private ModuleInterface[] requires;
  private ModuleInterface[] provides;
  private RoutingEntry[] routingEntries;
  private Permission[] permissionSets;
  private String[] modulePermissions; /* DEPRECATED */
  private UiModuleDescriptor uiDescriptor;
  private LaunchDescriptor launchDescriptor;

  private String tenantInterface; /* DEPRECATED */

  public ModuleDescriptor() {
  }

  /**
   * Copy constructor.
   *
   * @param other
   */
  public ModuleDescriptor(ModuleDescriptor other) {
    this.id = other.id;
    this.name = other.name;
    this.tags = other.tags;
    this.env = other.env;
    this.routingEntries = other.routingEntries;
    this.requires = other.requires;
    this.provides = other.provides;
    this.permissionSets = other.permissionSets;
    this.modulePermissions = other.modulePermissions;
    this.uiDescriptor = other.uiDescriptor;
    this.launchDescriptor = other.launchDescriptor;
    this.tenantInterface = other.tenantInterface;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonIgnore
  public String getNameOrId() {
    if (name != null && !name.isEmpty()) {
      return name;
    }
    return id;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public EnvEntry[] getEnv() {
    return env;
  }

  public void setEnv(EnvEntry[] env) {
    this.env = env;
  }

  public ModuleInterface[] getRequires() {
    return requires;
  }

  public void setRequires(ModuleInterface[] requires) {
    this.requires = requires;
  }

  public ModuleInterface[] getProvides() {
    return provides;
  }

  public void setProvides(ModuleInterface[] provides) {
    this.provides = provides;
  }

  public RoutingEntry[] getRoutingEntries() {
    return routingEntries;
  }

  public void setRoutingEntries(RoutingEntry[] routingEntries) {
    this.routingEntries = routingEntries;
  }

  /**
   * Get all RoutingEntries that are type proxy. Either from provided
   * interfaces, or from the global level RoutingEntries.
   *
   * @return
   */
  @JsonIgnore
  public List<RoutingEntry> getProxyRoutingEntries() {
    return getAllRoutingEntries("proxy", true);
  }

  /**
   * Get all routingEntries of given type.
   *
   * @param type "proxy" or "system" or "" for all types
   * @param globaltoo true: include the global-level entries too
   * @return a list of RoutingEntries
   */
  @JsonIgnore
  public List<RoutingEntry> getAllRoutingEntries(String type, boolean globaltoo) {
    List<RoutingEntry> all = new ArrayList<>();
    RoutingEntry[] res = getRoutingEntries();
    if (res != null && globaltoo) {
      Collections.addAll(all, res);
    }
    ModuleInterface[] prov = getProvides();
    if (prov != null) {
      for (ModuleInterface mi : prov) {
        String t = mi.getInterfaceType();
        if (t == null || t.isEmpty()) {
          t = "proxy";
        }
        if (type.isEmpty() || type.equals(t)) {
          res = mi.getRoutingEntries();
          if (res != null) {
            Collections.addAll(all, res);
          }
        }
      }
    }
    return all;
  }

  /**
   * Validate some features of a ModuleDescriptor.
   *
   * @return "" if ok, otherwise an informative error message.
   */
  public String validate() {
    if (getId() == null || getId().isEmpty()) {
      return "No Id in module";
    }
    if (!getId().matches("^[a-z0-9._-]+$")) {
      return "Invalid id";
    }
    List<RoutingEntry> all = getAllRoutingEntries("", true);
    if (all != null) {
      for (RoutingEntry e : all) {
        // TODO - Validate RoutingEntry in its own module
        String t = e.getType();
        if (!(t.equals("request-only")
          || (t.equals("request-response"))
          || (t.equals("headers"))
          || (t.equals("redirect"))
          || (t.equals("system")))) {
          return "Bad routing entry type: '" + t + "'";
        }
      }
    }
    if (getProvides() != null) {
      for (ModuleInterface pr : getProvides()) {
        String it = pr.getInterfaceType();
        if (it != null && !it.equals("proxy") && !it.equals("system")) {
          return "Bad interface type '" + it + "'";
        }
        // TODO - Validate version numbers and id
      }
    }
    // TODO - Validate requires section, no RoutingEntgries there,
    if (getTenantInterface() != null) {
      logger.warn("Module uses DEPRECATED tenantInterface field. "
        + "Provide a 'tenant' system interface instead");
      // Can not return error yet, need to accept this.
    }
    return "";
  }

  public String[] getModulePermissions() {
    return modulePermissions;
  }

  public void setModulePermissions(String[] modulePermissions) {
    this.modulePermissions = modulePermissions;
  }

  public UiModuleDescriptor getUiDescriptor() {
    return uiDescriptor;
  }

  public void setUiDescriptor(UiModuleDescriptor uiDescriptor) {
    this.uiDescriptor = uiDescriptor;
  }

  public LaunchDescriptor getLaunchDescriptor() {
    return launchDescriptor;
  }

  public void setLaunchDescriptor(LaunchDescriptor launchDescriptor) {
    this.launchDescriptor = launchDescriptor;
  }

  public String getTenantInterface() {
    return tenantInterface;
  }

  public void setTenantInterface(String tenantInterface) {
    this.tenantInterface = tenantInterface;
  }

  public Permission[] getPermissionSets() {
    return permissionSets;
  }

  public void setPermissionSets(Permission[] permissionSets) {
    this.permissionSets = permissionSets;
  }

}
