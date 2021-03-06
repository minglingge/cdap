/*
 * Copyright © 2016-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.internal.app.runtime.entity;

import co.cask.cdap.AllProgramsApp;
import co.cask.cdap.common.NotFoundException;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.entity.EntityExistenceVerifier;
import co.cask.cdap.common.id.Id;
import co.cask.cdap.common.test.AppJarHelper;
import co.cask.cdap.internal.AppFabricTestHelper;
import co.cask.cdap.internal.app.runtime.artifact.ArtifactRepository;
import co.cask.cdap.proto.NamespaceMeta;
import co.cask.cdap.proto.id.ApplicationId;
import co.cask.cdap.proto.id.ArtifactId;
import co.cask.cdap.proto.id.EntityId;
import co.cask.cdap.proto.id.InstanceId;
import co.cask.cdap.proto.id.NamespaceId;
import co.cask.cdap.store.NamespaceStore;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.apache.twill.filesystem.LocalLocationFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Tests for {@link EntityExistenceVerifier}.
 */
public class EntityExistenceTest {
  @ClassRule
  public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();
  private static EntityExistenceVerifier<EntityId> existenceVerifier;
  private static final String EXISTS = "exists";
  private static final String DOES_NOT_EXIST = "doesNotExist";
  private static final NamespaceId NAMESPACE = new NamespaceId(EXISTS);
  private static final ArtifactId ARTIFACT = NAMESPACE.artifact(EXISTS, "1");

  @BeforeClass
  public static void setup() throws Exception {
    CConfiguration cConf = CConfiguration.create();
    cConf.set(Constants.INSTANCE_NAME, EXISTS);
    Injector injector = AppFabricTestHelper.getInjector(cConf);
    NamespaceStore nsStore = injector.getInstance(NamespaceStore.class);
    ArtifactRepository artifactRepository = injector.getInstance(ArtifactRepository.class);
    cConf = injector.getInstance(CConfiguration.class);
    nsStore.create(new NamespaceMeta.Builder().setName(EXISTS).build());
    existenceVerifier = injector.getInstance(Key.get(new TypeLiteral<EntityExistenceVerifier<EntityId>>() { }));
    LocalLocationFactory lf = new LocalLocationFactory(TEMPORARY_FOLDER.newFolder());
    File artifactFile = new File(AppJarHelper.createDeploymentJar(lf, AllProgramsApp.class).toURI());
    artifactRepository.addArtifact(Id.Artifact.fromEntityId(ARTIFACT), artifactFile);
    AppFabricTestHelper.deployApplication(Id.Namespace.fromEntityId(NAMESPACE), AllProgramsApp.class, null, cConf);
  }

  @AfterClass
  public static void tearDown() {
    AppFabricTestHelper.shutdown();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExists() throws NotFoundException {
    existenceVerifier.ensureExists(new InstanceId(EXISTS));
    existenceVerifier.ensureExists(NAMESPACE);
    existenceVerifier.ensureExists(ARTIFACT);
    ApplicationId app = NAMESPACE.app(AllProgramsApp.NAME);
    existenceVerifier.ensureExists(app);
    existenceVerifier.ensureExists(app.mr(AllProgramsApp.NoOpMR.NAME));
    existenceVerifier.ensureExists(NAMESPACE.dataset(AllProgramsApp.DATASET_NAME));
  }

  @Test
  public void testDoesNotExist() {
    assertDoesNotExist(new InstanceId(DOES_NOT_EXIST));
    assertDoesNotExist(new NamespaceId(DOES_NOT_EXIST));
    assertDoesNotExist(NamespaceId.DEFAULT.artifact(DOES_NOT_EXIST, "1.0"));
    ApplicationId app = NamespaceId.DEFAULT.app(AllProgramsApp.NAME);
    assertDoesNotExist(NamespaceId.DEFAULT.app(DOES_NOT_EXIST));
    assertDoesNotExist(app.mr(DOES_NOT_EXIST));
    assertDoesNotExist(NamespaceId.DEFAULT.dataset(DOES_NOT_EXIST));
  }

  @SuppressWarnings("unchecked")
  private void assertDoesNotExist(EntityId entityId) {
    try {
      existenceVerifier.ensureExists(entityId);
      Assert.fail(String.format("Entity %s is not expected to exist but it does.", entityId));
    } catch (NotFoundException expected) {
      // expected
    }
  }
}
