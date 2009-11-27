import se.qbranch.qanban.User
import se.qbranch.qanban.Role

/**
 * User controller.
 */
class UserController {

    def authenticateService
    def sessionRegistry

    // the delete, save and update actions only accept POST requests
    static Map allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']

    def index = {
        redirect action: list, params: params
    }

    def list = {
        if (!params.max) {
            params.max = 10
        }
        [personList: User.list(params)]
    }

    def show = {
        def person = User.get(params.id)
        if (!person) {
            flash.message = "User not found with id $params.id"
            redirect action: list
            return
        }
        List roleNames = []
        for (role in person.authorities) {
            roleNames << role.authority
        }
        roleNames.sort { n1, n2 ->
            n1 <=> n2
        }
        [person: person, roleNames: roleNames]
    }

    /**
     * Person delete action. Before removing an existing person,
     * he should be removed from those authorities which he is involved.
     */
    def delete = {

        def person = User.get(params.id)
        if (person) {
            def authPrincipal = authenticateService.principal()
            //avoid self-delete if the logged-in user is an admin
            if (!(authPrincipal instanceof String) && authPrincipal.username == person.username) {
                flash.message = "You can not delete yourself, please login as another admin and try again"
            }
            else {
                //first, delete this person from People_Authorities table.
                Role.findAll().each { it.removeFromPeople(person) }
                person.delete()
                flash.message = "User $params.id deleted."
            }
        }
        else {
            flash.message = "User not found with id $params.id"
        }

        redirect action: list
    }

    def edit = {

        def person = User.get(params.id)
        if (!person) {
            flash.message = "User not found with id $params.id"
            redirect action: list
            return
        }

        return buildPersonModel(person)
    }

    /**
     * Person update action.
     */
    def update = {

        def person = User.get(params.id)
        if (!person) {
            flash.message = "User not found with id $params.id"
            redirect action: edit, id: params.id
            return
        }

        long version = params.version.toLong()
        if (person.version > version) {
            person.errors.rejectValue 'version', "person.optimistic.locking.failure",
				"Another user has updated this User while you were editing."
            render view: 'edit', model: buildPersonModel(person)
            return
        }

        def oldPassword = person.passwd
        person.properties = params
        if (!params.passwd.equals(oldPassword)) {
            person.passwd = authenticateService.encodePassword(params.passwd)
        }
        if (person.save()) {
            Role.findAll().each { it.removeFromPeople(person) }
            addRoles(person)
            redirect action: show, id: person.id
        }
        else {
            render view: 'edit', model: buildPersonModel(person)
        }
    }

    def create = {
        render(template: '/user/create', model: [person: new User(params)])
    }

    /**
     * Person save action.
     */
    def save = {
        def person = new User()
        person.properties = params
        person.passwd = authenticateService.encodePassword(params.passwd)
        if (person.save()) {
            //Varför funkar inte detta?
            flash.message = "${person.username} is now created"
            redirect(controller:'login',action:'auth')
                        
        }
        else {
            flash.message = "${person.username} already exists"
            redirect(controller:'login',action:'auth')
        }
    }

    def showOnlineUsers = {
        def users = sessionRegistry.getAllPrincipals()
        def onlineUsers = []

        for(user in users) {
            def userObject = User.findByUsername(user)
            onlineUsers.add(userObject)
        }

        render(template:'onlineUsers',model:[onlineUsers:onlineUsers])
    }

    private void addRoles(person) {
        for (String key in params.keySet()) {
            if (key.contains('ROLE') && 'on' == params.get(key)) {
                Role.findByAuthority(key).addToPeople(person)
            }
        }
    }

    private Map buildPersonModel(person) {

        List roles = Role.list()
        roles.sort { r1, r2 ->
            r1.authority <=> r2.authority
        }
        Set userRoleNames = []
        for (role in person.authorities) {
            userRoleNames << role.authority
        }
        LinkedHashMap<Role, Boolean> roleMap = [:]
        for (role in roles) {
            roleMap[(role)] = userRoleNames.contains(role.authority)
        }

        return [person: person, roleMap: roleMap]
    }
}