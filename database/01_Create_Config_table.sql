CREATE TABLE configuratie
(
  "id" serial NOT NULL,
  "property" character varying(255),
  "propval" character varying,
  "setting" character varying(255),
  "type" character varying(255)
);

insert into configuratie (property, propval, setting, "type") values ('useCookies', 'true', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('multipleActiveThemas', 'true', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('dataframepopupHandle', 'null', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('showLeftPanel', 'false', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('autoRedirect', '2', 'default', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('useSortableFunction', 'false', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('layerDelay', '5000', 'default', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('refreshDelay', '1000', 'default', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('minBboxZoeken', '1000', 'default', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('zoekConfigIds', '"1"', 'default', 'java.lang.String');
insert into configuratie (property, propval, setting, "type") values ('maxResults', '25', 'default', 'java.lang.Integer');
insert into configuratie (property, propval, setting, "type") values ('usePopup', 'false', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('useDivPopup', 'false', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('usePanelControls', 'true', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('expandAll', 'true', 'default', 'java.lang.Boolean');
insert into configuratie (property, propval, setting, "type") values ('tabs', '"themas", "legenda", "zoeken"', 'default', 'java.lang.String');
insert into configuratie (property, propval, setting, "type") values ('tolerance', '1', 'default', 'java.lang.Integer');