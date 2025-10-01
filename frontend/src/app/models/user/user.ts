export class User {

    id?:string;
    pseudo!:string;
    lastname!:string;
    firstname!:string;
    password!:string;
    email!:string;
    dateOfBirth!:string;
  

    constructor(
        pseudo: string,
        lastname: string,
        firstname: string,
        password: string,
        email: string,
        dateOfBirth: string,
      ) {
        this.pseudo = pseudo;
        this.lastname = lastname;
        this.firstname = firstname;
        this.password = password;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
      }
     
}
