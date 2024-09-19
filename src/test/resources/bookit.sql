select firstname,lastname,role
from users
where email = 'raymond@cydeo.com';

select id, name from team
where id =21899;

select *
from team t
inner join campus c on t.campus_id=c.id
where t.id =21899;


