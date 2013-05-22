/*
 * Copyright 2013 Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>.
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
package ca.corefacility.bioinformatics.irida.web.controller.api;

import ca.corefacility.bioinformatics.irida.model.*;
import ca.corefacility.bioinformatics.irida.model.enums.Order;
import ca.corefacility.bioinformatics.irida.model.roles.impl.Identifier;
import ca.corefacility.bioinformatics.irida.service.ProjectService;
import ca.corefacility.bioinformatics.irida.service.RelationshipService;
import ca.corefacility.bioinformatics.irida.service.UserService;
import ca.corefacility.bioinformatics.irida.web.assembler.resource.ResourceCollection;
import ca.corefacility.bioinformatics.irida.web.assembler.resource.project.ProjectResource;
import ca.corefacility.bioinformatics.irida.web.assembler.resource.user.UserResource;
import ca.corefacility.bioinformatics.irida.web.controller.links.LabelledRelationshipResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Controller for managing {@link Project}s in the database.
 *
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 */
@Controller
@ExposesResourceFor(Project.class)
@RequestMapping(value = "/projects")
public class ProjectsController extends GenericController<Identifier, Project, ProjectResource> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ProjectsController.class);
    /**
     * rel used for accessing users associated with a project.
     */
    private static final String PROJECT_USERS_REL = "project/users";
    /**
     * rel used for removing user relationships from a project.
     */
    private static final String PROJECT_USERS_DELETE_REL = "project/users/delete";
    /**
     * A label that's used to list the users associated with a project.
     */
    private static final String PROJECT_USERS_MAP_LABEL = "users";
    /**
     * A label that's used to list the samples associated with a project.
     */
    private static final String PROJECT_SAMPLES_MAP_LABEL = "samples";
    /**
     * A label that's used to list the sequence files associated with a project.
     */
    private static final String PROJECT_SEQUENCE_FILES_MAP_LABEL = "sequenceFiles";
    /**
     * Reference to {@link UserService} for getting users associated with a project.
     */
    private UserService userService;
    /**
     * Reference to {@link RelationshipService} for getting related resources.
     */
    private RelationshipService relationshipService;
    /**
     * Reference to {@link ProjectService} for managing projects.
     */
    private ProjectService projectService;

    /**
     * Constructor for {@link ProjectsController}, requires a reference to a {@link ProjectService}.
     *
     * @param projectService the {@link ProjectService} to be used by this controller.
     */
    @Autowired
    public ProjectsController(ProjectService projectService, UserService userService, RelationshipService relationshipService) {
        super(projectService, Project.class, Identifier.class, ProjectResource.class);
        this.userService = userService;
        this.relationshipService = relationshipService;
    }

    /**
     * A default constructor is required for the convenience <code>methodOn</code> method that the Spring HATEOAS
     * project provides. This method *should not* be used by anyone.
     */
    protected ProjectsController() {
        super(null, null, null, null);
    }

    /**
     * Get all users associated with a project.
     *
     * @param projectId the project id to get users for.
     * @return a model with a collection of user resources.
     */
    @RequestMapping(value = "/{projectId}/users", method = RequestMethod.GET)
    public ModelMap getUsersForProject(@PathVariable String projectId) {
        Identifier id = new Identifier();
        id.setIdentifier(projectId);
        ResourceCollection<UserResource> resources = new ResourceCollection<>();

        // get all of the users belonging to this project
        Collection<Relationship> relationships = userService.getUsersForProject(id);

        // for each of those relationships, retrieve the complete user object
        // and convert to a resource suitable for sending back to the client.
        for (Relationship r : relationships) {
            logger.debug(r.getSubject().getIdentifier());
            User u = userService.getUserByUsername(r.getSubject().getIdentifier());
            UserResource ur = new UserResource(u);
            ur.add(linkTo(UsersController.class).slash(u.getIdentifier().getIdentifier()).withSelfRel());
            resources.add(ur);
        }

        // add a link to this resource to the response
        resources.add(
                linkTo(methodOn(ProjectsController.class, String.class).getUsersForProject(projectId)).withSelfRel());

        // prepare the response for the client
        ModelMap model = new ModelMap();
        model.addAttribute(GenericController.RESOURCE_NAME, resources);

        return model;
    }

    /**
     * Add a relationship between a {@link Project} and a {@link User}.
     *
     * @param representation the JSON key-value pair that contains the identifier for the project and the identifier for
     *                       the user.
     * @return a response indicating that the collection was modified.
     */
    @RequestMapping(value = "/{projectId}/users", method = RequestMethod.PATCH)
    public ResponseEntity<String> addUserToProject(@PathVariable String projectId,
                                                   @RequestBody Map<String, Object> representation) {

        // first, get the project
        Identifier id = new Identifier();
        id.setIdentifier(projectId);
        Project p = projectService.read(id);
        User u = userService.getUserByUsername(representation.get("userIdentifier").toString());
        Role r = new Role();
        r.setName("ROLE_USER");
        projectService.addUserToProject(p, u, r);

        return new ResponseEntity<>("success", HttpStatus.OK);
    }

    /**
     * Projects should be sorted in descending order by default.
     *
     * @return a descending sort order (<code>Order.DESCENDING</code>).
     */
    @Override
    protected Order getDefaultSortOrder() {
        return Order.DESCENDING;
    }

    /**
     * Map a {@link ProjectResource} to a {@link Project}.
     *
     * @param pr the resource to map.
     * @return an instance of {@link Project}.
     */
    @Override
    protected Project mapResourceToType(ProjectResource pr) {
        Project p = new Project();
        p.setName(pr.getName());
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Class<?>> getUniquelyRelatedClasses() {
        Map<String, Class<?>> relatedResources = new HashMap<>();
        relatedResources.put(PROJECT_SAMPLES_MAP_LABEL, Sample.class);
        relatedResources.put(PROJECT_SEQUENCE_FILES_MAP_LABEL, SequenceFile.class);
        return relatedResources;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, Collection<LabelledRelationshipResource>> constructCustomRelatedResourceCollections(Project project) {
        Map<String, Collection<LabelledRelationshipResource>> resources = new HashMap<>();

        resources.put(PROJECT_USERS_MAP_LABEL, getUsersForProject(project));

        return resources;
    }

    /**
     * Get the users for this project as a collection of {@link ca.corefacility.bioinformatics.irida.web.controller.links.LabelledRelationshipResource}.
     *
     * @param project the project to get the users for.
     * @return labels and identifiers for the users attached to the project.
     */
    private Collection<LabelledRelationshipResource> getUsersForProject(Project project) {
        Collection<Relationship> relationships = userService.getUsersForProject(project.getIdentifier());
        List<LabelledRelationshipResource> userResources = new ArrayList<>(relationships.size());
        for (Relationship r : relationships) {
            Identifier userIdentifier = r.getSubject();
            LabelledRelationshipResource resource = new LabelledRelationshipResource(userIdentifier.getLabel(), r);
            // rel pointing at the user instance
            resource.add(linkTo(UsersController.class).slash(userIdentifier.getIdentifier()).withSelfRel());
            // rel telling the client how to delete the relationship between the user and the project.
            resource.add(linkTo(RelationshipsController.class).slash(r.getIdentifier().getIdentifier())
                    .withRel(PROJECT_USERS_DELETE_REL));
            userResources.add(resource);
        }
        return userResources;
    }

    /**
     * The {@link ProjectsController} should tell the client how to find the users for a specific {@link Project}.
     *
     * @param p the {@link Project} to construct custom links for.
     * @return a collection of custom links.
     */
    @Override
    protected Collection<Link> constructCustomResourceLinks(Project p) {
        Collection<Link> links = new HashSet<>();
        links.add(linkTo(ProjectsController.class).
                slash(p.getIdentifier().getIdentifier()).slash("users").
                withRel(PROJECT_USERS_REL));

        return links;
    }
}
