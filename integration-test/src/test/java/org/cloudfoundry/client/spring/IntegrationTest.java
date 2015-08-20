/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.spring;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpacesResponse;
import org.cloudfoundry.client.v3.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v3.applications.CreateApplicationResponse;
import org.cloudfoundry.client.v3.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v3.applications.DeleteApplicationResponse;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.GetPackageResponse;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.client.v3.packages.UploadPackageResponse;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import rx.Observable;

import java.io.File;

import static org.cloudfoundry.client.v3.packages.CreatePackageRequest.PackageType.BITS;

public final class IntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile String application;

    private volatile File bits;

    private volatile CloudFoundryClient client;

    private volatile String space;

    @Before
    public void configure() throws Exception {
        RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(new StandardEnvironment(), null);

        this.application = resolver.getRequiredProperty("test.application");
        this.bits = resolver.getRequiredProperty("test.bits", File.class);
        this.space = resolver.getRequiredProperty("test.space");

        this.client = new SpringCloudFoundryClientBuilder()
                .withApi(resolver.getRequiredProperty("test.host"))
                .withCredentials(
                        resolver.getRequiredProperty("test.username"),
                        resolver.getRequiredProperty("test.password"))
                .build();
    }

    @Test
    public void test() throws InterruptedException {
        listApplications()
                .flatMap(this::split)
                .flatMap(this::deleteApplication)
                .subscribe(response -> {
                        }, this::handleError,
                        () -> this.logger.info("All existing applications deleted"));

        listSpaces()
                .flatMap(this::createApplication)
                .flatMap(this::createPackage)
                .flatMap(this::uploadBits)
                .flatMap(this::waitForUploadProcessing)
                .first()
                .subscribe(response -> {
                    this.logger.info(response.getState());
                }, this::handleError);
    }

    private Observable<CreateApplicationResponse> createApplication(ListSpacesResponse response) {
        Resource.Metadata metadata = response.getResources().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find space " + this.space))
                .getMetadata();

        CreateApplicationRequest request = new CreateApplicationRequest()
                .withSpaceId(metadata.getId())
                .withName(this.application);

        return this.client.applications().create(request);
    }

    private Observable<CreatePackageResponse> createPackage(CreateApplicationResponse response) {
        CreatePackageRequest request = new CreatePackageRequest()
                .withApplicationId(response.getId())
                .withType(BITS);

        return this.client.packages().create(request);
    }

    private Observable<DeleteApplicationResponse> deleteApplication(ListApplicationsResponse.Resource resource) {
        DeleteApplicationRequest request = new DeleteApplicationRequest()
                .withId(resource.getId());

        return this.client.applications().delete(request);
    }

    private Observable<GetPackageResponse> waitForUploadProcessing(UploadPackageResponse uploadPackageResponse) {
        return Observable.<GetPackageResponse>create(subscriber -> {
            for (; ; ) {
                GetPackageRequest request = new GetPackageRequest()
                        .withId(uploadPackageResponse.getId());

                this.client.packages().get(request)
                        .subscribe(subscriber::onNext);

                if (subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                    break;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).filter(getPackageResponse -> "READY".equals(getPackageResponse.getState()));
    }

    private void handleError(Throwable exception) {
        this.logger.error("Error encountered: {}", exception.getMessage());
    }

    private Observable<ListApplicationsResponse> listApplications() {
        return this.client.applications().list(new ListApplicationsRequest());
    }

    private Observable<ListSpacesResponse> listSpaces() {
        ListSpacesRequest request = new ListSpacesRequest()
                .filterByName(this.space);

        return this.client.spaces().list(request);
    }

    private Observable<ListApplicationsResponse.Resource> split(ListApplicationsResponse response) {
        return Observable.from(response.getResources());
    }

    private Observable<UploadPackageResponse> uploadBits(CreatePackageResponse response) {
        UploadPackageRequest request = new UploadPackageRequest()
                .withId(response.getId())
                .withFile(this.bits);

        return this.client.packages().upload(request);
    }

}