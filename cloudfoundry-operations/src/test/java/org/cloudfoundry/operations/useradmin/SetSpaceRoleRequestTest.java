/*
 * Copyright 2013-2019 the original author or authors.
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

package org.cloudfoundry.operations.useradmin;

import org.junit.Test;

public final class SetSpaceRoleRequestTest {

    @Test(expected = IllegalStateException.class)
    public void noOrganizationName() {
        SetSpaceRoleRequest.builder()
            .spaceName("test-space")
            .spaceRole(SpaceRole.AUDITOR)
            .username("test-username")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void noSpaceName() {
        SetSpaceRoleRequest.builder()
            .organizationName("test-organization")
            .spaceRole(SpaceRole.MANAGER)
            .username("test-username")
            .build();
    }


    @Test(expected = IllegalStateException.class)
    public void noSpaceRole() {
        SetSpaceRoleRequest.builder()
            .organizationName("test-organization")
            .spaceName("test-space")
            .username("test-username")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void noUsername() {
        SetSpaceRoleRequest.builder()
            .organizationName("test-organization")
            .spaceName("test-space")
            .spaceRole(SpaceRole.DEVELOPER)
            .build();
    }

    @Test
    public void valid() {
        SetSpaceRoleRequest.builder()
            .organizationName("test-organization")
            .spaceName("test-space")
            .spaceRole(SpaceRole.AUDITOR)
            .username("test-username")
            .build();
    }

}
