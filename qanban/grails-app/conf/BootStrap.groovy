/*
 * Copyright 2009 Qbranch AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import se.qbranch.qanban.*

import grails.util.GrailsUtil

class BootStrap {

    def authenticateService
    def authenticationManager
    def sessionController
    def eventService

    Role userRole
    Role adminRole
    User adminUser
    User setupUser

    def init = { servletContext ->
        authenticationManager.sessionController = sessionController


        switch (GrailsUtil.environment) {

            case "test":


                adminRole = addRoleIfNotExist("administrator access", "ROLE_ADMIN")
                adminUser = addUserIfNotExist("testuser", "Mr Test", "test", true, "testing, testing...", "test@test.com", [adminRole])
                addTestBoardIfNotExist()

            break

            case 'development':

                adminRole = addRoleIfNotExist("administrator access", "ROLE_QANBANADMIN")
                userRole = addRoleIfNotExist("regular user access", "ROLE_QANBANUSER")
                adminUser = addUserIfNotExist("admin", "Admin User", "AdminPassword", true, "This is an admin user", "patrik.gardeman@gmail.com", [adminRole, userRole])
                setupUser = addUserIfNotExist("bootstrap", "Setup Deamon", "bs", true, "User creating the default board setup", "bootstrap@qanban.se",[adminRole, userRole])

                addBoardIfNotExist()
                adminRole.removeFromPeople(setupUser)
                userRole.removeFromPeople(setupUser)
                Event.findByDomainId(setupUser.domainId).delete()
                setupUser.delete()

                eventService.persist(new CardEventCreate(title: "Deploy QANBAN to Production",
                                                         caseNumber: 123,
                                                         description: "Log into Hudson environment and start the build 'Release to Prod'.",
                                                         phaseDomainId: (Phase.get(1).domainId),
                                                         eventCreator: adminUser))
                eventService.persist(new CardEventCreate(title: "Prepare QANBAN demo",
                                                         caseNumber: 456,
                                                         description: "Talk to the team about the new features and add them to the demo instructions.",
                                                         phaseDomainId: (Phase.get(1).domainId),
                                                         eventCreator: adminUser))

            break

            case 'production':

                adminRole = addRoleIfNotExist("administrator access", "ROLE_QANBANADMIN")
                userRole = addRoleIfNotExist("regular eventCreator access", "ROLE_QANBANUSER")
                setupUser = addUserIfNotExist("bootstrap", "Setup Deamon", "bs", true, "User creating the default board setup", "bootstrap@qanban.se",[adminRole, userRole])
                addBoardIfNotExist()
                adminRole.removeFromPeople(setupUser)
                userRole.removeFromPeople(setupUser)
                Event.findByDomainId(setupUser.domainId).delete()
                setupUser.delete()

        }

    }
    def destroy = {
    }

    private Role addRoleIfNotExist(desc, authority){
        def roleCheck = Role.findByAuthority(authority)
        if( !roleCheck )
            return new Role(description: desc, authority: authority).save()
        else
            return roleCheck
    }

    private User addUserIfNotExist(username, userRealName, passwd, enabled, description, email, authorities){

        def user = User.findByUsername(username)

        if( !user ){
            def createEvent = new UserEventCreate(
                username: username,
                userRealName:userRealName,
                passwd: passwd,
                passwdRepeat: passwd,
                enabled:enabled,
                description:description,
                email:email
            )

            eventService.persist(createEvent)

            user = createEvent.eventCreator

            // Only for development when we don't get the roles from the AD
            user.authorities = authorities

            if(user.save()) {
                for(role in authorities) {
                    role.addToPeople(user)
                }
            }
        }

        return user
    }

    private void addBoardIfNotExist(){

        if( Board.list().size() == 0 ){
            def bec = new BoardEventCreate(eventCreator:setupUser,title:'The Board')
            eventService.persist(bec)
            def board = bec.board
            eventService.persist(new PhaseEventCreate(title: "Backlog", phasePos: 0, cardLimit: 0, board: board, eventCreator: setupUser))
            eventService.persist(new PhaseEventCreate(title: "WIP", phasePos: 1, cardLimit: 5, eventCreator: setupUser, board: board))
            eventService.persist(new PhaseEventCreate(title: "Done", phasePos: 2, cardLimit: 0, eventCreator: setupUser, board: board))
            eventService.persist(new PhaseEventCreate(title: "Archive", phasePos: 3, cardLimit: 0, eventCreator: setupUser, board: board))
        }
    }


    private void addTestBoardIfNotExist() {
        if( Board.list().size() == 0 ){
            def bec = new BoardEventCreate(eventCreator:adminUser,title:'The Board')
            eventService.persist(bec)
            def board = bec.board
            eventService.persist(new PhaseEventCreate(title: "Backlog", phasePos: 0, cardLimit: 10, eventCreator: adminUser, board: board))
            eventService.persist(new PhaseEventCreate(title: "WIP", phasePos: 1, cardLimit: 5, eventCreator: adminUser, board: board))
            eventService.persist(new PhaseEventCreate(title: "Done", phasePos: 2, cardLimit: 5, eventCreator: adminUser, board: board))
            eventService.persist(new CardEventCreate(title: "Card #1", caseNumber: 1, description:"blalbblalbabla",phaseDomainId: (Phase.get(1).domainId), eventCreator: adminUser))
            eventService.persist(new CardEventCreate(title: "Card #2", caseNumber: 2, description:"blöblöblöblöbl",phaseDomainId: (Phase.get(1).domainId), eventCreator: adminUser))
        }
    }
}
