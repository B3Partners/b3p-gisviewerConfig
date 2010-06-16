CREATE TABLE configuratie
(
  "id" serial NOT NULL,
  "property" character varying(255),
  "propval" character varying,
  "setting" character varying(255),
  "type" character varying(255)
);

insert into configuratie (property, propval, setting, "type") values ('useCookies', 'true', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('usePopup', 'false', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('useDivPopup', 'false', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('usePanelControls', 'true', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('multipleActiveThemas', 'false', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('dataframepopupHandle', 'false', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('showLeftPanel', 'null', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('tolerance', '1', 'viewer', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('autoRedirect', '2', 'viewer', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('useSortableFunction', 'false', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('layerDelay', '5000', 'viewer', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('refreshDelay', '1000', 'viewer', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('minBboxZoeken', '1000', 'viewer', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('maxResults', '25', 'viewer', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('expandAll', 'true', 'viewer', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('zoekConfigIds', '"1,2,7"', 'viewer', 'java.lang.String');