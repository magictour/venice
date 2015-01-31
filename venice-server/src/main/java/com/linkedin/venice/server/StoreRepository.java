package com.linkedin.venice.server;

import com.linkedin.venice.store.AbstractStorageEngine;
import com.linkedin.venice.store.QueryStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;


/**
 *  A wrapper class that holds all the server's stores, storage engines
 *
 * There are two maps -
 * 1. localStorageEngines - is the lowest level persistence - mainly used by kafka consumer tasks
 * 2. localStores - These are Store abstractions over the local storage engines. They may be used by other services like for example Venice Broker.
 *    They provide access to only QueryStore APIs.
 *
 *  TODO 1. Later need to add stats and monitoring
 *
 */
public class StoreRepository {

  private static final Logger logger = Logger.getLogger(StoreRepository.class.getName());

  /**
   * All Stores owned by this node. Note this only allows for querying the store. No write operations are allowed
   */
  private final ConcurrentMap<String, QueryStore> localStores;

  /**
   *   Local storage engine for this node. This is lowest level persistence abstraction, these StorageEngines provide an iterator over their values.
   */
  private final ConcurrentMap<String, AbstractStorageEngine> localStorageEngines;

  public StoreRepository() {
    this.localStores = new ConcurrentHashMap<String, QueryStore>();
    this.localStorageEngines = new ConcurrentHashMap<String, AbstractStorageEngine>();
  }

  /*
    Usual CRUD operations on map of Local Stores
   */
  public boolean hasLocalStore(String name) {
    return this.localStores.containsKey(name);
  }

  public QueryStore getLocalStore(String storeName) {
    return this.localStores.get(storeName);
  }

  private QueryStore removeLocalStore(String storeName) {
    return this.localStores.remove(storeName);
  }

  private void addLocalStore(QueryStore store)
      throws Exception {
    QueryStore found = this.localStores.putIfAbsent(store.getName(), store);

    if (found != null) {
      String errorMessage = "Store '" + store.getName() + "' has already been initialized.";
      logger.error(errorMessage);
      throw new Exception(errorMessage); // TODO change to appropriate Exception type later
    }
  }

  public List<QueryStore> getAllLocalStores() {
    return new ArrayList<QueryStore>(this.localStores.values());
  }

  /*
  Usual CRUD operations on map of Local Storage Engines
   */
  public boolean hasLocalStorageEngine(String name) {
    return this.localStorageEngines.containsKey(name);
  }

  public AbstractStorageEngine getLocalStorageEngine(String storeName) {
    return this.localStorageEngines.get(storeName);
  }

  public AbstractStorageEngine removeLocalStorageEngine(String storeName) {
    this.removeLocalStore(storeName);
    return this.localStorageEngines.remove(storeName);
  }

  public void addLocalStorageEngine(AbstractStorageEngine engine)
      throws Exception {
    AbstractStorageEngine found = this.localStorageEngines.putIfAbsent(engine.getName(), engine);
    if (found != null) {
      String errorMessage = "Storage Engine '" + engine.getName() + "' has already been initialized.";
      logger.error(errorMessage);
      throw new Exception(errorMessage); // TODO change to appropriate Exception type later
    }
    this.addLocalStore((QueryStore) engine);
  }

  public List<AbstractStorageEngine> getAllLocalStorageEngines() {
    return new ArrayList<AbstractStorageEngine>(this.localStorageEngines.values());
  }
}
